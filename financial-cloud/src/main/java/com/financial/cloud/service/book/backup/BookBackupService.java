package com.financial.cloud.service.book.backup;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.book.BookService;
import com.financial.cloud.service.history.HistorySystemLogsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipOutputStream;

/**
 * 账套备份导出：按 BackupTableRegistry 清单逐表导出为 ZIP（manifest.json + data/<table>.jsonl）。
 * 对应 openspec/changes/book-backup-restore/design.md §3。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookBackupService {

    public static final String FORMAT = "financial-cloud-book-backup";
    public static final int FORMAT_VERSION = 2;
    public static final String CONTENT_TYPE = "application/zip";

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JdbcTemplate jdbcTemplate;
    private final BookMapper bookMapper;
    private final BookService bookService;
    private final HistorySystemLogsService historySystemLogsService;

    public record BackupPackage(byte[] content, String encodedFileName) {
    }

    /** 导出当前账套的完整业务备份包。调用方必须为本账套管理员。 */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BackupPackage export(String bookId, UserInfo operator) {
        bookService.requireBookAdministrator(operator, bookId);
        return doExport(bookId, operator, "export", "导出账套备份");
    }

    /**
     * 系统调度导出：同格式 ZIP，不校验交互式账套管理员会话。
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BackupPackage exportForSystem(String bookId) {
        UserInfo system = new UserInfo();
        system.setId("scheduled-backup");
        system.setUsername("scheduled-backup");
        return doExport(bookId, system, "scheduled-export", "定时导出账套备份");
    }

    private BackupPackage doExport(String bookId, UserInfo operator, String action, String auditPrefix) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(400, "当前账套不存在或不可访问");
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ArrayNode tableManifests = MAPPER.createArrayNode();
            ArrayNode fileManifests = MAPPER.createArrayNode();
            Set<String> fileIds = new LinkedHashSet<>();
            BackupArchive archive = new BackupArchive();
            long totalRows = 0;
            byte[] zipBytes;
            try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
                for (BackupTableSpec spec : BackupTableRegistry.SPECS) {
                    byte[] jsonl = exportTable(spec, bookId);
                    int rows = countLines(jsonl);
                    totalRows += rows;
                    archive.write(zip, "data/" + spec.table() + ".jsonl", jsonl);
                    if (spec.table().equals("voucher_attachment") || spec.table().equals("expense_claim_attachment")) {
                        for (Map<String, Object> row : BackupJsonCodec.decodeRows(jsonl)) {
                            Object id = row.get("file_id");
                            if (id == null || String.valueOf(id).isBlank()) {
                                throw new BusinessException(400, "备份附件缺少文件引用，无法完整导出");
                            }
                            fileIds.add(String.valueOf(id));
                        }
                    }

                    ObjectNode tableNode = MAPPER.createObjectNode();
                    tableNode.put("name", spec.table());
                    tableNode.put("rows", rows);
                    tableNode.put("sha256", sha256(jsonl));
                    tableManifests.add(tableNode);
                }

                for (String fileId : fileIds) {
                    List<Map<String, Object>> stored = jdbcTemplate.queryForList("SELECT * FROM file_storage WHERE id=?", fileId);
                    if (stored.size() != 1 || !(stored.get(0).get("data_stored") instanceof byte[] bytes)) {
                        throw new BusinessException(400, "备份附件文件已丢失，无法完整导出：" + fileId);
                    }
                    // A corrupted shared reference must not export another book's confidential attachment.
                    List<Map<String, Object>> owners = jdbcTemplate.queryForList(
                            "SELECT book_id FROM voucher_attachment WHERE file_id=? UNION SELECT book_id FROM expense_claim_attachment WHERE file_id=?", fileId, fileId);
                    if (owners.stream().anyMatch(row -> !bookId.equals(String.valueOf(row.get("book_id"))))) {
                        throw new BusinessException(400, "备份附件文件存在跨账套引用，无法导出：" + fileId);
                    }
                    String entry = "files/" + fileManifests.size() + ".bin";
                    archive.write(zip, entry, bytes);
                    Map<String, Object> file = stored.get(0);
                    ObjectNode node = MAPPER.createObjectNode();
                    node.put("id", fileId);
                    node.put("entry", entry);
                    node.put("size", bytes.length);
                    node.put("sha256", sha256(bytes));
                    node.put("fileName", file.get("file_name") == null ? null : String.valueOf(file.get("file_name")));
                    node.put("contentType", file.get("content_type") == null ? null : String.valueOf(file.get("content_type")));
                    node.put("category", file.get("category") == null ? null : String.valueOf(file.get("category")));
                    fileManifests.add(node);
                }

                ObjectNode manifest = buildManifest(book, tableManifests);
                manifest.set("files", fileManifests);
                archive.write(zip, "manifest.json", MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
            }
            zipBytes = buffer.toByteArray();

            String fileName = "book-backup-" + safeFileName(book.getName())
                    + "-" + FILE_TS.format(LocalDateTime.now()) + ".zip";
            audit(book, operator, action, "success",
                    auditPrefix + "：" + BackupTableRegistry.SPECS.size() + " 表，" + totalRows + " 行");
            return new BackupPackage(zipBytes,
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));
        } catch (IOException e) {
            throw new BusinessException(500, "账套备份导出失败：" + e.getMessage());
        }
    }

    /** 供定时落盘使用的安全文件名片段。 */
    public static String safeBookFileName(String name) {
        return safeFileName(name);
    }

    private byte[] exportTable(BackupTableSpec spec, String bookId) {
        String sql = switch (spec.scope()) {
            case BOOK_ID -> "SELECT * FROM `" + spec.table() + "` WHERE book_id = ?"
                    + (spec.hasDeleted() ? " AND deleted = 'n'" : "")
                    + (spec.extraCondition() != null ? " AND " + spec.extraCondition() : "");
            case VIA_VOUCHER -> "SELECT * FROM `" + spec.table() + "` WHERE voucher_id IN "
                    + "(SELECT id FROM voucher WHERE book_id = ? AND deleted = 'n')"
                    + (spec.hasDeleted() ? " AND deleted = 'n'" : "");
            case BOOK_ROW -> throw new IllegalStateException("账套主表不走逐表导出：" + spec.table());
        };
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, bookId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map<String, Object> row : rows) {
            byte[] line = BackupJsonCodec.encodeRow(row);
            out.write(line, 0, line.length);
        }
        return out.toByteArray();
    }

    private ObjectNode buildManifest(Book book, ArrayNode tables) {
        ObjectNode manifest = MAPPER.createObjectNode();
        manifest.put("format", FORMAT);
        manifest.put("formatVersion", FORMAT_VERSION);
        manifest.put("exportedAt", LocalDateTime.now().toString());

        ObjectNode bookNode = manifest.putObject("book");
        bookNode.put("name", book.getName());
        bookNode.put("companyName", book.getCompanyName());
        bookNode.put("creditCode", book.getCreditCode());
        bookNode.put("address", book.getAddress());
        bookNode.put("industry", book.getIndustry());
        bookNode.put("vatType", book.getVatType());
        bookNode.put("voucherReviewed", book.getVoucherReviewed());
        bookNode.put("standardId", book.getStandardId());
        bookNode.put("enableDate", book.getEnableDate() == null ? null : book.getEnableDate().toString());
        bookNode.put("currentAccountDate",
                book.getCurrentAccountDate() == null ? null : book.getCurrentAccountDate().toString());

        manifest.set("tables", tables);
        return manifest;
    }

    private void audit(Book book, UserInfo operator, String action, String result, String message) {
        try {
            historySystemLogsService.log("账套备份", book.getId(), book.getName(), null,
                    message, action, result, operator, null);
        } catch (Exception e) {
            log.warn("备份审计日志写入失败：{}", e.getMessage());
        }
    }

    static int countLines(byte[] jsonl) {
        int lines = 0;
        for (byte b : jsonl) {
            if (b == '\n') {
                lines++;
            }
        }
        return lines;
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String safeFileName(String name) {
        return name == null ? "book" : name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
}
