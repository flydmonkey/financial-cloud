package com.financial.cloud.service.book.backup;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.financial.cloud.configuration.BookBackupScheduleProperties;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.context.WebContext;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.enums.book.BookStatusEnum;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.book.BookService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 账套备份恢复：克隆式恢复 + 覆盖式恢复（强确认 + 预备份）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookRestoreService {

    public static final String OVERWRITE_CONFIRM_PHRASE = "覆盖恢复";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JdbcTemplate jdbcTemplate;
    private final BookMapper bookMapper;
    private final IdentifierGenerator identifierGenerator;
    private final PermissionBookService permissionBookService;
    private final RoleMemberService roleMemberService;
    private final HistorySystemLogsService historySystemLogsService;
    private final BookService bookService;
    private final BookBackupService bookBackupService;
    private final BookBackupScheduleProperties scheduleProperties;

    public record RestoreResult(String bookId, String name, int tables, long rows) {
    }

    public record OverwriteResult(String bookId, String name, int tables, long rows, String preBackupFile) {
    }

    private record ParsedBackup(JsonNode bookMeta, Map<String, List<Map<String, Object>>> tableRows) {
    }

    /**
     * 克隆式恢复：校验通过后恢复为一个新账套，绝不覆盖现有账套。
     */
    @Transactional
    public RestoreResult restore(InputStream zipStream, UserInfo operator) {
        ParsedBackup backup = parseAndValidate(zipStream);

        Book book = createBookShell(backup.bookMeta());
        String newBookId = book.getId();
        grantOperatorAccess(book, operator);

        long totalRows = insertRemappedTables(backup, newBookId);

        audit(book, operator, "restore", "success",
                "恢复账套备份：" + BackupTableRegistry.SPECS.size() + " 表，" + totalRows + " 行");
        return new RestoreResult(newBookId, book.getName(),
                BackupTableRegistry.SPECS.size(), totalRows);
    }

    /**
     * 覆盖式恢复：清空目标账套业务表后灌入备份（保留 book_id 与成员授权）。
     * 必须先通过确认短语；覆盖前落盘预备份。
     */
    @Transactional
    public OverwriteResult overwrite(String targetBookId, InputStream zipStream,
                                     String confirmPhrase, UserInfo operator) {
        if (!OVERWRITE_CONFIRM_PHRASE.equals(confirmPhrase == null ? "" : confirmPhrase.trim())) {
            throw new BusinessException(400, "请输入确认短语「" + OVERWRITE_CONFIRM_PHRASE + "」以继续覆盖恢复");
        }
        bookService.requireBookAdministrator(operator, targetBookId);
        Book target = bookMapper.selectById(targetBookId);
        if (target == null) {
            throw new BusinessException(400, "目标账套不存在或不可访问");
        }
        if (BookStatusEnum.isSealed(target.getStatus())) {
            throw new BusinessException(400, "封存账套不可覆盖恢复，请先解除封存");
        }

        ParsedBackup backup = parseAndValidate(zipStream);
        String preBackupFile = writePreOverwriteBackup(targetBookId, operator);

        wipeBookBusinessData(targetBookId);
        applyBookMeta(target, backup.bookMeta(), true);
        bookMapper.updateById(target);

        long totalRows = insertRemappedTables(backup, targetBookId);

        audit(target, operator, "overwrite-restore", "success",
                "覆盖恢复账套：" + BackupTableRegistry.SPECS.size() + " 表，" + totalRows
                        + " 行；预备份=" + preBackupFile);
        return new OverwriteResult(targetBookId, target.getName(),
                BackupTableRegistry.SPECS.size(), totalRows, preBackupFile);
    }

    private long insertRemappedTables(ParsedBackup backup, String bookId) {
        Map<String, Map<String, String>> idMaps = new HashMap<>();
        long totalRows = 0;
        for (BackupTableSpec spec : BackupTableRegistry.SPECS) {
            List<Map<String, Object>> rows = backup.tableRows().getOrDefault(spec.table(), List.of());
            Map<String, String> idMap = idMaps.computeIfAbsent(spec.table(), k -> new HashMap<>());
            for (Map<String, Object> row : rows) {
                Object oldPk = row.get(spec.pk());
                String newPk = identifierGenerator.nextId(spec.table()).toString();
                if (oldPk != null) {
                    idMap.put(String.valueOf(oldPk), newPk);
                }
                row.put(spec.pk(), newPk);
            }
            for (Map<String, Object> row : rows) {
                rewriteRow(spec, row, bookId, idMaps);
                insertRow(spec.table(), row);
                totalRows++;
            }
        }
        return totalRows;
    }

    private String writePreOverwriteBackup(String bookId, UserInfo operator) {
        try {
            BookBackupService.BackupPackage pack = bookBackupService.export(bookId, operator);
            Path dir = Paths.get(scheduleProperties.getDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String fileName = "pre-overwrite-" + bookId + "-" + FILE_TS.format(LocalDateTime.now()) + ".zip";
            Path target = dir.resolve(fileName);
            Files.write(target, pack.content());
            log.info("覆盖恢复预备份已写入 {}", target);
            return target.toString();
        } catch (IOException e) {
            throw new BusinessException(500, "覆盖前自动备份失败，已中止覆盖：" + e.getMessage());
        }
    }

    void wipeBookBusinessData(String bookId) {
        List<BackupTableSpec> reverse = new ArrayList<>(BackupTableRegistry.SPECS);
        Collections.reverse(reverse);
        for (BackupTableSpec spec : reverse) {
            switch (spec.scope()) {
                case BOOK_ID -> jdbcTemplate.update(
                        "DELETE FROM `" + spec.table() + "` WHERE book_id = ?", bookId);
                case VIA_VOUCHER -> jdbcTemplate.update(
                        "DELETE t FROM `" + spec.table() + "` t "
                                + "INNER JOIN voucher v ON t.voucher_id = v.id WHERE v.book_id = ?",
                        bookId);
                case BOOK_ROW -> {
                    // 账套主表不删除
                }
            }
        }
    }

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
                throw new BusinessException(400, "备份包缺少表声明：" + spec.table());
            }
            byte[] jsonl = entries.get("data/" + spec.table() + ".jsonl");
            if (jsonl == null) {
                throw new BusinessException(400, "备份包缺少数据文件：" + spec.table());
            }
            if (!BookBackupService.sha256(jsonl).equals(tableNode.path("sha256").asText())) {
                throw new BusinessException(400, "备份包校验失败：" + spec.table() + " 内容与清单不符");
            }
            List<Map<String, Object>> rows;
            try {
                rows = BackupJsonCodec.decodeRows(jsonl);
            } catch (RuntimeException e) {
                throw new BusinessException(400, "备份包解析失败：" + spec.table() + " — " + e.getMessage());
            }
            if (rows.size() != tableNode.path("rows").asInt(-1)) {
                throw new BusinessException(400, "备份包行数不符：" + spec.table());
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

    private Book createBookShell(JsonNode meta) {
        Book book = new Book();
        book.setId(identifierGenerator.nextId(book).toString());
        book.setName(meta.path("name").asText() + "（备份恢复）");
        applyBookMeta(book, meta, false);
        book.setStatus(1);
        bookMapper.insert(book);
        return book;
    }

    private void applyBookMeta(Book book, JsonNode meta, boolean overwriteName) {
        if (overwriteName) {
            book.setName(meta.path("name").asText());
        }
        book.setCompanyName(textOrNull(meta, "companyName"));
        book.setCreditCode(textOrNull(meta, "creditCode"));
        book.setAddress(textOrNull(meta, "address"));
        book.setIndustry(meta.path("industry").isNumber() ? meta.path("industry").intValue() : null);
        book.setVatType(meta.path("vatType").isNumber() ? meta.path("vatType").intValue() : 0);
        book.setVoucherReviewed(meta.path("voucherReviewed").isNumber() ? meta.path("voucherReviewed").intValue() : 0);
        book.setStandardId(textOrNull(meta, "standardId"));
        book.setEnableDate(parseYearMonth(textOrNull(meta, "enableDate")));
        book.setCurrentAccountDate(parseYearMonth(textOrNull(meta, "currentAccountDate")));
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
                if (edge.keepOnMiss()) {
                    // 哨兵值保留
                } else if (edge.soft()) {
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
