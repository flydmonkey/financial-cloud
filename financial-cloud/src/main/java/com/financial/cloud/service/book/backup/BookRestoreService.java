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
import java.util.Set;
import java.util.HashSet;

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

    private record ParsedBackup(JsonNode bookMeta, Map<String, List<Map<String, Object>>> tableRows,
                                Map<String, Map<String, Object>> files) {
    }

    /**
     * 克隆式恢复：校验通过后恢复为一个新账套，绝不覆盖现有账套。
     */
    @Transactional
    public RestoreResult restore(InputStream zipStream, UserInfo operator) {
        bookService.requireBookAdministrator(operator, operator == null ? null : operator.getBookId());
        ParsedBackup backup = parseAndValidate(zipStream, operator);

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

        ParsedBackup backup = parseAndValidate(zipStream, operator);
        String preBackupFile = writePreOverwriteBackup(targetBookId, operator);

        wipeBookBusinessData(targetBookId);
        // The destination's identity (including its name) survives overwrite.
        // Copying the source name creates duplicate names with the still-existing source book.
        applyBookMeta(target, backup.bookMeta(), false);
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
        Map<String, String> fileIds = new HashMap<>();
        // Never share physical file rows with the source book: attachment deletion must be local.
        for (Map.Entry<String, Map<String, Object>> entry : backup.files().entrySet()) {
            Map<String, Object> file = new LinkedHashMap<>(entry.getValue());
            String id = identifierGenerator.nextId("file_storage").toString();
            file.put("id", id);
            insertRow("file_storage", file);
            fileIds.put(entry.getKey(), id);
        }
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
                if (isAttachment(spec.table())) {
                    row.put("file_id", fileIds.get(String.valueOf(row.get("file_id"))));
                }
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

    private ParsedBackup parseAndValidate(InputStream zipStream, UserInfo operator) {
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
        int version = manifest.path("formatVersion").asInt(-1);
        if (version != 1 && version != BookBackupService.FORMAT_VERSION) {
            throw new BusinessException(400, "备份包格式版本不受支持：" + manifest.path("formatVersion").asText());
        }

        Map<String, JsonNode> declared = new HashMap<>();
        if (!manifest.path("tables").isArray()) {
            throw new BusinessException(400, "备份包缺少有效表清单");
        }
        for (JsonNode tableNode : manifest.path("tables")) {
            String name = tableNode.path("name").asText();
            if (BackupTableRegistry.SPECS.stream().noneMatch(spec -> spec.table().equals(name))
                    || declared.putIfAbsent(name, tableNode) != null) {
                throw new BusinessException(400, "备份包包含未知或重复表声明：" + name);
            }
        }
        Set<String> expectedEntries = new HashSet<>(Set.of("manifest.json"));
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
            expectedEntries.add("data/" + spec.table() + ".jsonl");
            Set<String> primaryKeys = new HashSet<>();
            for (Map<String, Object> row : rows) {
                Object pk = row.get(spec.pk());
                if (pk == null || String.valueOf(pk).isBlank() || !primaryKeys.add(String.valueOf(pk))
                        || row.keySet().stream().anyMatch(column -> !column.matches("[a-z][a-z0-9_]*"))) {
                    throw new BusinessException(400, "备份包数据列名或主键无效：" + spec.table());
                }
            }
        }
        JsonNode bookMeta = manifest.path("book");
        if (bookMeta.isMissingNode() || bookMeta.path("name").asText().isBlank()) {
            throw new BusinessException(400, "备份包缺少账套元信息");
        }
        Map<String, Map<String, Object>> files = new LinkedHashMap<>();
        if (version == 2) {
            files = parsePortableFiles(manifest, entries, tableRows, expectedEntries);
        } else {
        for (String table : List.of("voucher_attachment", "expense_claim_attachment")) {
            for (Map<String, Object> row : tableRows.get(table)) {
                String fileId = String.valueOf(row.get("file_id"));
                if (files.containsKey(fileId)) continue;
                List<Map<String, Object>> owners = jdbcTemplate.queryForList(
                        "SELECT book_id FROM voucher_attachment WHERE file_id=? UNION SELECT book_id FROM expense_claim_attachment WHERE file_id=?",
                        fileId, fileId);
                if (owners.isEmpty()) {
                    throw new BusinessException(400, "备份附件缺少可验证的来源账套");
                }
                for (Map<String, Object> owner : owners) {
                    bookService.requireBookAdministrator(operator, String.valueOf(owner.get("book_id")));
                }
                List<Map<String, Object>> stored = jdbcTemplate.queryForList(
                        "SELECT * FROM file_storage WHERE id=?", fileId);
                if (stored.size() != 1 || stored.get(0).get("data_stored") == null) {
                    throw new BusinessException(400, "备份附件文件已丢失，无法恢复");
                }
                files.put(fileId, stored.get(0));
            }
        }
        }
        if (!expectedEntries.equals(entries.keySet())) {
            throw new BusinessException(400, "备份包包含未声明的 ZIP 项");
        }
        return new ParsedBackup(bookMeta, tableRows, files);
    }

    private Map<String, Map<String, Object>> parsePortableFiles(JsonNode manifest, Map<String, byte[]> entries,
            Map<String, List<Map<String, Object>>> tables, Set<String> expectedEntries) {
        if (!manifest.path("files").isArray()) {
            throw new BusinessException(400, "v2备份包缺少附件文件清单");
        }
        Set<String> referenced = new HashSet<>();
        for (String table : List.of("voucher_attachment", "expense_claim_attachment")) {
            for (Map<String, Object> row : tables.get(table)) {
                Object id = row.get("file_id");
                if (id == null || String.valueOf(id).isBlank()) {
                    throw new BusinessException(400, "备份附件缺少文件引用");
                }
                referenced.add(String.valueOf(id));
            }
        }
        Map<String, Map<String, Object>> files = new LinkedHashMap<>();
        for (JsonNode node : manifest.path("files")) {
            String id = node.path("id").asText();
            String entry = node.path("entry").asText();
            if (!referenced.contains(id) || files.containsKey(id) || !entry.matches("files/[0-9]+\\.bin")
                    || !expectedEntries.add(entry)) {
                throw new BusinessException(400, "备份附件包含无效或重复文件声明");
            }
            byte[] bytes = entries.get(entry);
            if (bytes == null || !node.path("size").isIntegralNumber()
                    || bytes.length != node.path("size").asLong(-1)
                    || !BookBackupService.sha256(bytes).equals(node.path("sha256").asText())) {
                throw new BusinessException(400, "备份附件校验失败：" + id);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("data_stored", bytes);
            row.put("content_size", bytes.length);
            row.put("file_name", fileMetadata(node, "fileName", 400));
            row.put("content_type", fileMetadata(node, "contentType", 100));
            row.put("category", fileMetadata(node, "category", 10));
            row.put("created_by", null);
            files.put(id, row);
        }
        if (!files.keySet().equals(referenced)) {
            throw new BusinessException(400, "备份附件缺少包内文件声明");
        }
        return files;
    }

    private static String fileMetadata(JsonNode node, String name, int maxLength) {
        JsonNode field = node.path(name);
        if (field.isNull() || field.isMissingNode()) return null;
        if (!field.isTextual() || field.asText().length() > maxLength) {
            throw new BusinessException(400, "备份附件元信息无效：" + name);
        }
        return field.asText();
    }

    private static boolean isAttachment(String table) {
        return "voucher_attachment".equals(table) || "expense_claim_attachment".equals(table);
    }

    private Map<String, byte[]> readZip(InputStream zipStream) {
        return BackupArchive.read(zipStream);
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
        if (spec.scope() == BackupTableSpec.Scope.BOOK_ID) {
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
                    if (!"template".equals(String.valueOf(value))) {
                        throw new BusinessException(400, "备份报表引用不属于包内数据或 template 哨兵");
                    }
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
