package com.financial.cloud.service.book.backup;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.context.WebContext;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.service.idm.RoleMemberService;
import com.financial.cloud.service.permissions.PermissionBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 账套备份恢复：克隆式恢复——校验通过后恢复为一个新账套，绝不覆盖现有账套。
 * 所有主键重新分配，FK 边按 BackupTableRegistry 声明重映射，单事务，失败整体回滚。
 * 对应 openspec/changes/book-backup-restore/design.md §4。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookRestoreService {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final JdbcTemplate jdbcTemplate;
    private final BookMapper bookMapper;
    private final IdentifierGenerator identifierGenerator;
    private final PermissionBookService permissionBookService;
    private final RoleMemberService roleMemberService;
    private final HistorySystemLogsService historySystemLogsService;

    public record RestoreResult(String bookId, String name, int tables, long rows) {
    }

    private record ParsedBackup(JsonNode bookMeta, Map<String, List<Map<String, Object>>> tableRows) {
    }

    /**
     * 恢复备份包。任何校验或写入失败都抛出异常并整体回滚，不产生残留数据。
     */
    @Transactional
    public RestoreResult restore(InputStream zipStream, UserInfo operator) {
        ParsedBackup backup = parseAndValidate(zipStream);

        Book book = createBookShell(backup.bookMeta());
        String newBookId = book.getId();
        grantOperatorAccess(book, operator);

        Map<String, Map<String, String>> idMaps = new HashMap<>();
        long totalRows = 0;
        for (BackupTableSpec spec : BackupTableRegistry.SPECS) {
            List<Map<String, Object>> rows = backup.tableRows().getOrDefault(spec.table(), List.of());
            Map<String, String> idMap = idMaps.computeIfAbsent(spec.table(), k -> new HashMap<>());
            // 第一遍：整表统一分配新主键（自引用表如 book_subject 的父行可能排在子行之后）
            for (Map<String, Object> row : rows) {
                Object oldPk = row.get(spec.pk());
                String newPk = identifierGenerator.nextId(spec.table()).toString();
                if (oldPk != null) {
                    idMap.put(String.valueOf(oldPk), newPk);
                }
                row.put(spec.pk(), newPk);
            }
            // 第二遍：改写 book_id / 外键 / 置空列后插入
            for (Map<String, Object> row : rows) {
                rewriteRow(spec, row, newBookId, idMaps);
                insertRow(spec.table(), row);
                totalRows++;
            }
        }

        audit(book, operator, "restore", "success",
                "恢复账套备份：" + BackupTableRegistry.SPECS.size() + " 表，" + totalRows + " 行");
        return new RestoreResult(newBookId, book.getName(),
                BackupTableRegistry.SPECS.size(), totalRows);
    }

    // ---------- 解析与校验（零写入） ----------

    private ParsedBackup parseAndValidate(InputStream zipStream) {
        Map<String, byte[]> entries = readZip(zipStream);
        byte[] manifestBytes = entries.get("manifest.json");
        if (manifestBytes == null) {
            throw new BusinessException(400, "备份包缺少 manifest.json，不是有效的账套备份");
        }
        JsonNode manifest;
        try {
            manifest = MAPPER.readTree(manifestBytes);
        } catch (tools.jackson.core.JacksonException e) {
            throw new BusinessException(400, "备份包 manifest 解析失败：" + e.getMessage());
        }
        if (!BookBackupService.FORMAT.equals(manifest.path("format").asText())) {
            throw new BusinessException(400, "备份包格式不符，无法恢复");
        }
        if (manifest.path("formatVersion").asInt(-1) != BookBackupService.FORMAT_VERSION) {
            throw new BusinessException(400, "备份包格式版本不受支持：" + manifest.path("formatVersion").asText());
        }

        Map<String, JsonNode> declared = new HashMap<>();
        for (JsonNode tableNode : manifest.path("tables")) {
            declared.put(tableNode.path("name").asText(), tableNode);
        }

        Map<String, List<Map<String, Object>>> tableRows = new LinkedHashMap<>();
        for (BackupTableSpec spec : BackupTableRegistry.SPECS) {
            JsonNode tableNode = declared.get(spec.table());
            if (tableNode == null) {
                throw new BusinessException(400, "备份包缺少数据表：" + spec.table());
            }
            byte[] jsonl = entries.get("data/" + spec.table() + ".jsonl");
            if (jsonl == null) {
                throw new BusinessException(400, "备份包缺少数据文件：" + spec.table() + ".jsonl");
            }
            if (!BookBackupService.sha256(jsonl).equals(tableNode.path("sha256").asText())) {
                throw new BusinessException(400, "备份包数据校验和不符（可能被篡改）：" + spec.table());
            }
            List<Map<String, Object>> rows;
            try {
                rows = BackupJsonCodec.decodeRows(jsonl);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(400, "备份包数据损坏：" + spec.table() + "，" + e.getMessage());
            }
            if (rows.size() != tableNode.path("rows").asInt(-1)) {
                throw new BusinessException(400, "备份包行数与清单不符：" + spec.table());
            }
            tableRows.put(spec.table(), rows);
        }
        JsonNode bookMeta = manifest.path("book");
        if (bookMeta.isMissingNode() || bookMeta.path("name").asText().isBlank()) {
            throw new BusinessException(400, "备份包缺少账套元信息");
        }
        return new ParsedBackup(bookMeta, tableRows);
    }

    private Map<String, byte[]> readZip(InputStream zipStream) {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int len;
                while ((len = zip.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                entries.put(entry.getName(), out.toByteArray());
            }
        } catch (IOException e) {
            throw new BusinessException(400, "备份包读取失败，请确认上传的是 ZIP 文件：" + e.getMessage());
        }
        return entries;
    }

    // ---------- 建壳与授权 ----------

    private Book createBookShell(JsonNode meta) {
        Book book = new Book();
        book.setId(identifierGenerator.nextId(book).toString());
        book.setName(meta.path("name").asText() + "（备份恢复）");
        book.setCompanyName(textOrNull(meta, "companyName"));
        book.setCreditCode(textOrNull(meta, "creditCode"));
        book.setAddress(textOrNull(meta, "address"));
        book.setIndustry(meta.path("industry").isNumber() ? meta.path("industry").intValue() : null);
        book.setVatType(meta.path("vatType").isNumber() ? meta.path("vatType").intValue() : 0);
        book.setVoucherReviewed(meta.path("voucherReviewed").isNumber() ? meta.path("voucherReviewed").intValue() : 0);
        book.setStandardId(textOrNull(meta, "standardId"));
        book.setEnableDate(parseYearMonth(textOrNull(meta, "enableDate")));
        book.setCurrentAccountDate(parseYearMonth(textOrNull(meta, "currentAccountDate")));
        book.setStatus(1);
        bookMapper.insert(book);
        return book;
    }

    private void grantOperatorAccess(Book book, UserInfo operator) {
        if (operator == null || operator.getId() == null) {
            return;
        }
        permissionBookService.save(new PermissionBook(operator.getId(), book.getId()));
        RoleMember adminMember = new RoleMember(ProductRoles.ADMINISTRATORS, operator.getId(), "USER", book.getId());
        adminMember.setId(WebContext.genId());
        roleMemberService.save(adminMember);
    }

    // ---------- 行重写与插入 ----------

    private void rewriteRow(BackupTableSpec spec, Map<String, Object> row, String newBookId,
                            Map<String, Map<String, String>> idMaps) {
        if (spec.scope() == BackupTableSpec.Scope.BOOK_ID && row.containsKey("book_id")) {
            row.put("book_id", newBookId);
        }
        for (String column : spec.nullOnRestore()) {
            if (row.containsKey(column)) {
                row.put(column, null);
            }
        }
        for (BackupTableSpec.FkEdge edge : spec.fkEdges()) {
            Object value = row.get(edge.column());
            if (value == null) {
                continue;
            }
            Map<String, String> refMap = idMaps.getOrDefault(edge.refTable(), Map.of());
            String mapped = refMap.get(String.valueOf(value));
            if (mapped == null) {
                if (edge.soft()) {
                    row.put(edge.column(), null);
                } else {
                    throw new BusinessException(500, "备份恢复失败：" + spec.table() + "." + edge.column()
                            + " 引用了不存在的数据（" + edge.refTable() + "=" + value + "）");
                }
            } else {
                row.put(edge.column(), mapped);
            }
        }
    }

    private void insertRow(String table, Map<String, Object> row) {
        List<String> columns = new ArrayList<>(row.keySet());
        String sql = "INSERT INTO `" + table + "` ("
                + columns.stream().map(c -> "`" + c + "`").reduce((a, b) -> a + "," + b).orElseThrow()
                + ") VALUES (" + String.join(",", columns.stream().map(c -> "?").toList()) + ")";
        jdbcTemplate.update(sql, columns.stream().map(row::get).toArray());
    }

    private void audit(Book book, UserInfo operator, String action, String result, String message) {
        try {
            historySystemLogsService.log("账套备份", book.getId(), book.getName(), null,
                    message, action, result, operator, null);
        } catch (Exception e) {
            log.warn("备份审计日志写入失败：{}", e.getMessage());
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private static YearMonth parseYearMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value.substring(0, 7));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
