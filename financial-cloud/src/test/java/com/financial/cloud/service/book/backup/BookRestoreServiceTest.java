package com.financial.cloud.service.book.backup;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.service.idm.RoleMemberService;
import com.financial.cloud.service.permissions.PermissionBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookRestoreServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private BookMapper bookMapper;
    @Mock private IdentifierGenerator identifierGenerator;
    @Mock private PermissionBookService permissionBookService;
    @Mock private RoleMemberService roleMemberService;
    @Mock private HistorySystemLogsService historyService;

    private BookRestoreService service;
    private final AtomicLong idSeq = new AtomicLong(1000);

    @BeforeEach
    void setUp() {
        service = new BookRestoreService(jdbcTemplate, bookMapper, identifierGenerator,
                permissionBookService, roleMemberService, historyService);
        lenient().when(identifierGenerator.nextId(any())).thenAnswer(inv -> idSeq.incrementAndGet());
        lenient().when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    }

    @Test
    void restoreCreatesNewBookAndRemapsForeignKeys() {
        Map<String, List<Map<String, Object>>> tableData = new LinkedHashMap<>();
        // 自引用表故意让父行排在子行之后，验证两遍灌数
        addRow(tableData, "book_subject", orderedMap("id", "sub-1", "book_id", "b-old", "parent_id", "sub-0"));
        addRow(tableData, "book_subject", orderedMap("id", "sub-0", "book_id", "b-old"));
        addRow(tableData, "voucher", orderedMap("id", "v-1", "book_id", "b-old",
                "audit_member_id", "user-9", "debit_amount", new BigDecimal("100.00")));
        addRow(tableData, "voucher_item", orderedMap("id", "vi-1", "book_id", "b-old",
                "voucher_id", "v-1", "subject_id", "sub-1"));

        BookRestoreService.RestoreResult result = service.restore(
                new ByteArrayInputStream(buildZip(tableData, Map.of(), null)), operator());

        assertThat(result.name()).endsWith("（备份恢复）");
        assertThat(result.rows()).isEqualTo(4);

        Map<String, List<Map<String, Object>>> inserted = captureInserts();
        String newBookId = result.bookId();

        // 科目：父行晚到也能正确重映射 parent_id
        List<Map<String, Object>> subjects = inserted.get("book_subject");
        Map<String, Object> child = subjects.stream().filter(r -> r.get("parent_id") != null).findFirst().orElseThrow();
        Map<String, Object> parent = subjects.stream().filter(r -> r.get("parent_id") == null).findFirst().orElseThrow();
        assertThat(child.get("parent_id")).isEqualTo(parent.get("id"));
        assertThat(child.get("book_id")).isEqualTo(newBookId);

        // 凭证：人员 ID 置空，book_id 指向新账套
        Map<String, Object> voucher = inserted.get("voucher").get(0);
        assertThat(voucher.get("book_id")).isEqualTo(newBookId);
        assertThat(voucher.get("audit_member_id")).isNull();
        assertThat(voucher.get("id")).isNotEqualTo("v-1");

        // 分录：voucher_id / subject_id 全部指向新 ID
        Map<String, Object> item = inserted.get("voucher_item").get(0);
        assertThat(item.get("voucher_id")).isEqualTo(voucher.get("id"));
        assertThat(item.get("subject_id")).isEqualTo(child.get("id"));
        assertThat(item.get("book_id")).isEqualTo(newBookId);
    }

    @Test
    void tamperedChecksumIsRejectedWithZeroWrites() {
        Map<String, List<Map<String, Object>>> tableData = new LinkedHashMap<>();
        byte[] zip = buildZip(tableData,
                Map.of("voucher", "{\"name\":\"voucher\",\"rows\":0,\"sha256\":\"deadbeef\"}"), null);

        assertThatThrownBy(() -> service.restore(new ByteArrayInputStream(zip), operator()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("校验和");
        verify(bookMapper, never()).insert(any(Book.class));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void missingTableFileIsRejected() {
        // manifest 声明全部表，但 ZIP 里删掉 config 的数据文件
        byte[] zip = buildZip(Map.of(), Map.of(), "config");
        assertThatThrownBy(() -> service.restore(new ByteArrayInputStream(zip), operator()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("缺少数据文件");
    }

    @Test
    void wrongFormatVersionIsRejected() {
        Map<String, List<Map<String, Object>>> tableData = new LinkedHashMap<>();
        byte[] zip = buildZip(tableData, Map.of(), null, "\"formatVersion\":99,");
        assertThatThrownBy(() -> service.restore(new ByteArrayInputStream(zip), operator()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("版本");
    }

    // ---------- 测试工具 ----------

    private UserInfo operator() {
        UserInfo user = new UserInfo();
        user.setId("user-1");
        user.setUsername("admin");
        return user;
    }

    private static Map<String, Object> orderedMap(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    private void addRow(Map<String, List<Map<String, Object>>> tableData, String table, Map<String, Object> row) {
        tableData.computeIfAbsent(table, k -> new ArrayList<>()).add(row);
    }

    /**
     * 用与真实导出相同的 JSONL 编码构造备份包；未列出的注册表自动补空文件。
     *
     * @param manifestOverrides 表名 → 清单条目 JSON 替换（篡改场景）
     * @param skipDataFile      非空时该表不写数据文件（缺文件场景）
     */
    private byte[] buildZip(Map<String, List<Map<String, Object>>> tableData,
                            Map<String, String> manifestOverrides, String skipDataFile) {
        return buildZip(tableData, manifestOverrides, skipDataFile, null);
    }

    private byte[] buildZip(Map<String, List<Map<String, Object>>> tableData,
                            Map<String, String> manifestOverrides, String skipDataFile,
                            String manifestPrefixPatch) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8);
            List<String> tableEntries = new ArrayList<>();
            for (BackupTableSpec spec : BackupTableRegistry.SPECS) {
                StringBuilder jsonl = new StringBuilder();
                for (Map<String, Object> row : tableData.getOrDefault(spec.table(), List.of())) {
                    jsonl.append(new String(BackupJsonCodec.encodeRow(row), StandardCharsets.UTF_8));
                }
                byte[] bytes = jsonl.toString().getBytes(StandardCharsets.UTF_8);
                if (!spec.table().equals(skipDataFile)) {
                    zip.putNextEntry(new ZipEntry("data/" + spec.table() + ".jsonl"));
                    zip.write(bytes);
                    zip.closeEntry();
                }
                String override = manifestOverrides.get(spec.table());
                tableEntries.add(override != null ? override
                        : "{\"name\":\"" + spec.table() + "\",\"rows\":" + BookBackupService.countLines(bytes)
                        + ",\"sha256\":\"" + BookBackupService.sha256(bytes) + "\"}");
            }
            String manifest = "{\"format\":\"" + BookBackupService.FORMAT + "\","
                    + (manifestPrefixPatch != null ? manifestPrefixPatch : "\"formatVersion\":1,")
                    + "\"exportedAt\":\"2026-09-29T02:00:00\","
                    + "\"book\":{\"name\":\"示例账套\",\"companyName\":\"示例科技有限公司\",\"standardId\":\"1\","
                    + "\"enableDate\":\"2026-01\",\"vatType\":0,\"voucherReviewed\":1},"
                    + "\"tables\":[" + String.join(",", tableEntries) + "]}";
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifest.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.close();
            return buffer.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 捕获所有 INSERT，解析为 表名 → [列→值] 行列表。 */
    private Map<String, List<Map<String, Object>>> captureInserts() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, atLeastOnce()).update(sqlCaptor.capture(), argsCaptor.capture());

        Map<String, List<Map<String, Object>>> inserted = new HashMap<>();
        List<String> sqls = sqlCaptor.getAllValues();
        List<Object[]> argValues = argsCaptor.getAllValues();
        for (int i = 0; i < sqls.size(); i++) {
            String sql = sqls.get(i);
            String table = sql.substring(sql.indexOf('`') + 1, sql.indexOf('`', sql.indexOf('`') + 1));
            String columnsPart = sql.substring(sql.indexOf('(') + 1, sql.indexOf(')'));
            String[] columns = columnsPart.replace("`", "").split(",");
            Object[] values = argValues.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            for (int c = 0; c < columns.length; c++) {
                row.put(columns[c].trim(), values[c]);
            }
            inserted.computeIfAbsent(table, k -> new ArrayList<>()).add(row);
        }
        return inserted;
    }
}
