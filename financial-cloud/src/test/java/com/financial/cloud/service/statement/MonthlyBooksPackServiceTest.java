package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.voucher.VoucherService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonthlyBooksPackServiceTest {

    @Mock private StatementReportService reportService;
    @Mock private StatementBalanceSheetService balanceSheetService;
    @Mock private StatementIncomeService incomeService;
    @Mock private VoucherService voucherService;
    @Mock private BookMapper bookMapper;

    private MonthlyBooksPackService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new MonthlyBooksPackService(reportService, balanceSheetService, incomeService,
                voucherService, bookMapper);
        Book book = new Book();
        book.setId("book-1");
        book.setName("示例账套");
        when(bookMapper.selectById("book-1")).thenReturn(book);
        Page<VoucherItemVo> page = new Page<>();
        VoucherItemVo row = new VoucherItemVo();
        row.setVoucherDate(new Date(0));
        row.setWord("记-1");
        row.setSummary("销售收入");
        row.setSubjectCode("1001");
        row.setSubjectName("库存现金");
        row.setDebitAmount(new BigDecimal("100.00"));
        row.setCreditAmount(BigDecimal.ZERO);
        page.setRecords(List.of(row));
        when(voucherService.subLedger(any())).thenReturn(Message.ok(page));
        doAnswer(invocation -> writeMarker(invocation.getArgument(1), "subject-balance"))
                .when(reportService).subjectBalanceExport(any(), any());
        doAnswer(invocation -> writeMarker(invocation.getArgument(1), "cash-flow"))
                .when(reportService).cashFlowExport(any(), any());
        doAnswer(invocation -> writeMarker(invocation.getArgument(1), "balance-sheet"))
                .when(balanceSheetService).export(any(), any());
        doAnswer(invocation -> writeMarker(invocation.getArgument(1), "income"))
                .when(incomeService).export(any(), any());
        doAnswer(invocation -> writeMarker(invocation.getArgument(1), "voucher-list"))
                .when(voucherService).export(any(), any());
    }

    @Test
    void buildsRequiredEntriesAndIndividualVoucherListByDefault() throws Exception {
        MonthlyBooksPackService.BooksPack pack = service.build("book-1", "2026-08", true, "user-1");

        assertThat(pack.fileName()).isEqualTo("示例账套_本月账本包_2026-08.zip");
        assertThat(MonthlyBooksPackService.entryNames(pack.content())).containsExactly(
                "科目余额表_2026-08.xlsx", "科目明细账_2026-08.xlsx", "资产负债表_2026-08.xlsx",
                "利润表_2026-08.xlsx", "现金流量表_2026-08.xlsx", "凭证清单_2026-08.xlsx");
        assertThat(unzip(pack.content()).get("科目余额表_2026-08.xlsx"))
                .isEqualTo("subject-balance".getBytes(StandardCharsets.UTF_8));
        verify(voucherService).export(any(), any());
    }

    @Test
    void omitsVoucherListWhenDisabled() throws Exception {
        MonthlyBooksPackService.BooksPack pack = service.build("book-1", "2026-08", false, "user-1");

        assertThat(MonthlyBooksPackService.entryNames(pack.content()))
                .doesNotContain("凭证清单_2026-08.xlsx").hasSize(5);
        verify(voucherService, never()).export(any(), any());
    }

    @Test
    void detailLedgerContainsVoucherLevelFieldsAndRunningBalance() throws Exception {
        byte[] workbookBytes = service.detailLedger("book-1", "2026-08");

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            var sheet = workbook.getSheet("科目明细账");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("凭证字号");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("记-1");
            assertThat(sheet.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(100D);
            assertThat(sheet.getRow(1).getCell(7).getStringCellValue()).isEqualTo("借");
            assertThat(sheet.getRow(1).getCell(8).getNumericCellValue()).isEqualTo(100D);
        }
    }

    @Test
    void rejectsMalformedPeriodBeforeWritingAnyZip() {
        assertThatThrownBy(() -> service.build("book-1", "2026-13", true, "user-1"))
                .hasMessageContaining("YYYY-MM");
    }

    @Test
    void rejectsAnInaccessibleCurrentBook() {
        when(bookMapper.selectById("missing-book")).thenReturn(null);

        assertThatThrownBy(() -> service.build("missing-book", "2026-08", true, "user-1"))
                .hasMessageContaining("不可访问");
    }

    @Test
    void memberFailureReturnsNoPartialPack() throws Exception {
        doAnswer(invocation -> { throw new java.io.IOException("boom"); })
                .when(incomeService).export(any(), any());

        assertThatThrownBy(() -> service.build("book-1", "2026-08", true, "user-1"))
                .hasMessageContaining("income");
    }

    private static Object writeMarker(HttpServletResponse response, String value) throws Exception {
        response.getOutputStream().write(value.getBytes(StandardCharsets.UTF_8));
        return null;
    }

    private static Map<String, byte[]> unzip(byte[] value) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(value), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                entries.put(entry.getName(), input.readAllBytes());
            }
        }
        return entries;
    }
}
