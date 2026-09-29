package com.financial.cloud.service.book.backup;

import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.book.BookService;
import com.financial.cloud.service.history.HistorySystemLogsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookBackupServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private BookMapper bookMapper;
    @Mock private BookService bookService;
    @Mock private HistorySystemLogsService historyService;

    private BookBackupService service;

    @BeforeEach
    void setUp() {
        service = new BookBackupService(jdbcTemplate, bookMapper, bookService, historyService);
        Book book = new Book();
        book.setId("book-1");
        book.setName("示例账套");
        book.setCompanyName("示例科技有限公司");
        lenient().when(bookMapper.selectById("book-1")).thenReturn(book);

        lenient().when(jdbcTemplate.queryForList(anyString(), eq("book-1"))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "old-1");
            row.put("book_id", "book-1");
            row.put("amount", new BigDecimal("12345.67"));
            row.put("created_date", Timestamp.valueOf("2026-09-01 10:00:00"));
            return List.of(row);
        });
    }

    @Test
    void exportProducesZipWithManifestAndAllTableFiles() throws Exception {
        BookBackupService.BackupPackage pack = service.export("book-1", new UserInfo());

        Map<String, byte[]> entries = unzip(pack.content());
        assertThat(entries).containsKey("manifest.json");
        for (BackupTableSpec spec : BackupTableRegistry.SPECS) {
            assertThat(entries).containsKey("data/" + spec.table() + ".jsonl");
        }

        String manifest = new String(entries.get("manifest.json"), StandardCharsets.UTF_8);
        assertThat(manifest).contains(BookBackupService.FORMAT);
        assertThat(manifest).contains("\"formatVersion\" : 1");
        assertThat(manifest).contains("示例账套");
        assertThat(manifest).contains("示例科技有限公司");
        // 每张表一行数据，清单行数与校验和必须可查
        assertThat(manifest).contains("\"rows\" : 1");
        assertThat(manifest).contains("sha256");

        // JSONL 内容：金额保持精度，时间转字符串
        String voucherLine = new String(entries.get("data/voucher.jsonl"), StandardCharsets.UTF_8);
        assertThat(voucherLine).contains("12345.67");
        assertThat(voucherLine).contains("2026-09-01 10:00:00");

        assertThat(pack.encodedFileName()).startsWith("book-backup-");
    }

    @Test
    void exportForSystemProducesZipWithoutAdminCheck() throws Exception {
        BookBackupService.BackupPackage pack = service.exportForSystem("book-1");

        Map<String, byte[]> entries = unzip(pack.content());
        assertThat(entries).containsKey("manifest.json");
        String manifest = new String(entries.get("manifest.json"), StandardCharsets.UTF_8);
        assertThat(manifest).contains(BookBackupService.FORMAT);
    }

    private Map<String, byte[]> unzip(byte[] zipBytes) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), zip.readAllBytes());
            }
        }
        return entries;
    }
}
