package com.financial.cloud.service.statement;

import com.financial.cloud.common.Message;
import com.financial.cloud.constants.system.ConstsSysConfig;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.statement.StatementIncome;
import com.financial.cloud.domain.statement.StatementIncomeItem;
import com.financial.cloud.dto.statement.StatementExpenseDetailItem;
import com.financial.cloud.dto.statement.StatementExpenseDetailReport;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.service.config.ConfigSysService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementExpenseDetailServiceTest {

    @Mock
    private BookSubjectMapper bookSubjectMapper;
    @Mock
    private VoucherItemMapper voucherItemMapper;
    @Mock
    private ConfigSysService configSysService;
    @Mock
    private StatementIncomeService statementIncomeService;

    @InjectMocks
    private StatementExpenseDetailService service;

    @BeforeEach
    void setUpReconciliationDependencies() {
        lenient().when(configSysService.getBookConfigMap("book-1")).thenReturn(Map.of(
                ConstsSysConfig.SYS_DEFAULT_SELLING_EXPENSES, "105",
                ConstsSysConfig.SYS_DEFAULT_ADMINISTRATIVE_EXPENSES, "106",
                ConstsSysConfig.SYS_DEFAULT_FINANCIAL_EXPENSES, "107"));
        lenient().when(statementIncomeService.generateIncomeStatement(any(), eq(false)))
                .thenReturn(Message.ok(income()));
    }

    @Test
    void query_buildsTreeAndTotals_withDefaultPrefixesAndPostedOnly() {
        StatementParamsDto dto = StatementParamsDto.builder()
                .bookId("book-1")
                .periodType("between")
                .dateRange(new String[]{"2023-01", "2023-02"})
                .subjectCodes(List.of())
                .build();
        BookSubject root = subject("root", null, "5601", "销售费用", 1);
        BookSubject leaf = subject("leaf", "root", "5601.01", "广告费", 2);
        BookSubject unrelated = subject("other", null, "1001", "库存现金", 1);
        VoucherItemVo january = amount("5601.01", "2023-01", "100", "0");
        VoucherItemVo february = amount("5601.01", "2023-02", "20", "5");

        when(bookSubjectMapper.selectList(any()))
                .thenReturn(List.of(root, leaf, unrelated));
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any()))
                .thenReturn(List.of(january, february));
        when(statementIncomeService.generateIncomeStatement(any(), eq(false)))
                .thenReturn(Message.ok(income(item("105", "100"))))
                .thenReturn(Message.ok(income(item("105", "15"))));

        Message<StatementExpenseDetailReport> message = service.query(dto);

        assertEquals(Message.SUCCESS, message.getCode());
        StatementExpenseDetailReport report = message.getData();
        assertNotNull(report);
        assertEquals(List.of("2023-01", "2023-02"), report.getPeriods());
        assertEquals("2023年合计", report.getYearLabel());
        assertTrue(report.getReconciliationWarnings().isEmpty());
        assertEquals(1, report.getItems().size());

        StatementExpenseDetailItem rootItem = report.getItems().get(0);
        assertEquals("5601", rootItem.getSubjectCode());
        assertEquals(0, new BigDecimal("100").compareTo(rootItem.getAmounts().get("2023-01")));
        assertEquals(0, new BigDecimal("15").compareTo(rootItem.getAmounts().get("2023-02")));
        assertEquals(0, new BigDecimal("115").compareTo(rootItem.getYearTotal()));
        assertEquals(1, rootItem.getChildren().size());
        assertEquals("5601.01", rootItem.getChildren().get(0).getSubjectCode());
        assertEquals(0, new BigDecimal("115").compareTo(report.getTotals().get("yearTotal")));

        assertEquals(Boolean.TRUE, dto.getPostedOnly());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> prefixesCaptor = ArgumentCaptor.forClass(List.class);
        verify(voucherItemMapper).selectExpenseAmountByMonth(same(dto), prefixesCaptor.capture());
        assertEquals(List.of("5601", "6601", "5602", "6602", "5603", "6603"),
                prefixesCaptor.getValue());
    }

    @Test
    void query_includesEnterpriseExpenseTree_withDefaultSubjectCodes() {
        StatementParamsDto dto = singlePeriodDto();
        BookSubject root = subject("root", null, "6601", "销售费用", 1);
        BookSubject leaf = subject("leaf", "root", "6601.01", "广告费", 2);
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(root, leaf));
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any()))
                .thenReturn(List.of(amount("6601.01", "2023-01", "88", "0")));

        StatementExpenseDetailReport report = service.query(dto).getData();

        assertEquals(1, report.getItems().size());
        assertEquals("6601", report.getItems().get(0).getSubjectCode());
        assertEquals(0, new BigDecimal("88").compareTo(report.getItems().get(0).getYearTotal()));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> prefixesCaptor = ArgumentCaptor.forClass(List.class);
        verify(voucherItemMapper).selectExpenseAmountByMonth(same(dto), prefixesCaptor.capture());
        assertTrue(prefixesCaptor.getValue().contains("6601"));
    }

    @Test
    void query_mergesExactAndAliasAmountMapsByPeriod() {
        StatementParamsDto dto = singlePeriodDto();
        BookSubject root = subject("root", null, "5601", "销售费用", 1);
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(root));
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any()))
                .thenReturn(List.of(
                        amount("5601", "2023-01", "10", "0"),
                        amount("6601", "2023-01", "20", "0")));

        StatementExpenseDetailReport report = service.query(dto).getData();

        assertEquals(1, report.getItems().size());
        assertEquals(0, new BigDecimal("30")
                .compareTo(report.getItems().get(0).getAmounts().get("2023-01")));
    }

    @Test
    void query_prefersSmallBusinessTree_whenBothExpenseFamiliesExist() {
        StatementParamsDto dto = singlePeriodDto();
        BookSubject smallBusinessRoot = subject("small", null, "5601", "销售费用", 1);
        BookSubject enterpriseRoot = subject("enterprise", null, "6601", "销售费用", 1);
        when(bookSubjectMapper.selectList(any()))
                .thenReturn(List.of(smallBusinessRoot, enterpriseRoot));
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any()))
                .thenReturn(List.of(
                        amount("5601", "2023-01", "10", "0"),
                        amount("6601", "2023-01", "20", "0")));
        when(statementIncomeService.generateIncomeStatement(any(), eq(false)))
                .thenReturn(Message.ok(income(item("105", "30"))));

        StatementExpenseDetailReport report = service.query(dto).getData();

        assertEquals(1, report.getItems().size());
        assertEquals("5601", report.getItems().get(0).getSubjectCode());
        assertEquals(0, new BigDecimal("30").compareTo(report.getTotals().get("2023-01")));
        assertTrue(report.getReconciliationWarnings().isEmpty());
    }

    @Test
    void query_expandsInclusiveSubjectCodeRange_forSqlPrefixes() {
        StatementParamsDto dto = singlePeriodDto();
        dto.setSubjectCodes(List.of("2121-2131"));
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of());
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any())).thenReturn(List.of());

        service.query(dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> prefixesCaptor = ArgumentCaptor.forClass(List.class);
        verify(voucherItemMapper).selectExpenseAmountByMonth(same(dto), prefixesCaptor.capture());
        List<String> prefixes = prefixesCaptor.getValue();
        assertFalse(prefixes.contains("2121-2131"));
        for (int code = 2121; code <= 2131; code++) {
            assertTrue(prefixes.contains(Integer.toString(code)));
        }
    }

    @Test
    void query_removesZeroRootAfterFilteringLeaves() {
        StatementParamsDto dto = singlePeriodDto();
        BookSubject zeroRoot = subject("zero", null, "5601", "销售费用", 1);
        BookSubject nonzeroRoot = subject("nonzero", null, "5602", "管理费用", 1);
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(zeroRoot, nonzeroRoot));
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any()))
                .thenReturn(List.of(amount("5602", "2023-01", "25", "0")));

        StatementExpenseDetailReport report = service.query(dto).getData();

        assertEquals(1, report.getItems().size());
        assertEquals("5602", report.getItems().get(0).getSubjectCode());
        assertFalse(report.getItems().stream()
                .anyMatch(item -> "5601".equals(item.getSubjectCode())));
    }

    @Test
    void query_reconciliationWarningsEmpty_whenExpenseDetailMatchesIncomeStatement() {
        StatementParamsDto dto = singlePeriodDto();
        BookSubject root = subject("root", null, "6601", "销售费用", 1);
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(root));
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any()))
                .thenReturn(List.of(amount("6601", "2023-01", "88", "0")));
        when(statementIncomeService.generateIncomeStatement(any(), eq(false)))
                .thenReturn(Message.ok(income(item("105", "88"))));

        StatementExpenseDetailReport report = service.query(dto).getData();

        assertTrue(report.getReconciliationWarnings().isEmpty());
        ArgumentCaptor<StatementParamsDto> monthDto = ArgumentCaptor.forClass(StatementParamsDto.class);
        verify(statementIncomeService).generateIncomeStatement(monthDto.capture(), eq(false));
        assertEquals("month", monthDto.getValue().getPeriodType());
        assertEquals("2023-01", monthDto.getValue().getReportDate());
        assertEquals(Boolean.TRUE, monthDto.getValue().getPostedOnly());
    }

    @Test
    void query_reconciliationWarningsNonEmpty_whenExpenseDetailDiffersFromIncomeStatement() {
        StatementParamsDto dto = singlePeriodDto();
        BookSubject root = subject("root", null, "5602", "管理费用", 1);
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(root));
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any()))
                .thenReturn(List.of(amount("5602", "2023-01", "25", "0")));
        when(statementIncomeService.generateIncomeStatement(any(), eq(false)))
                .thenReturn(Message.ok(income(item("106", "20"))));

        StatementExpenseDetailReport report = service.query(dto).getData();

        assertEquals(1, report.getReconciliationWarnings().size());
        assertEquals("5602", report.getReconciliationWarnings().get(0).getSubjectCode());
        assertEquals("2023-01", report.getReconciliationWarnings().get(0).getPeriod());
        assertEquals(0, new BigDecimal("5")
                .compareTo(report.getReconciliationWarnings().get(0).getDiff()));
    }

    @Test
    void query_reconcilesDetailAgainstZero_whenIncomeItemConfigIsMissing() {
        StatementParamsDto dto = singlePeriodDto();
        BookSubject root = subject("root", null, "5601", "销售费用", 1);
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(root));
        when(voucherItemMapper.selectExpenseAmountByMonth(same(dto), any()))
                .thenReturn(List.of(amount("5601", "2023-01", "12", "0")));
        when(configSysService.getBookConfigMap("book-1")).thenReturn(Map.of(
                ConstsSysConfig.SYS_DEFAULT_ADMINISTRATIVE_EXPENSES, "106",
                ConstsSysConfig.SYS_DEFAULT_FINANCIAL_EXPENSES, "107"));

        StatementExpenseDetailReport report = service.query(dto).getData();

        assertEquals(1, report.getReconciliationWarnings().size());
        assertEquals("5601", report.getReconciliationWarnings().get(0).getSubjectCode());
        assertEquals("2023-01", report.getReconciliationWarnings().get(0).getPeriod());
        assertEquals(0, new BigDecimal("12")
                .compareTo(report.getReconciliationWarnings().get(0).getDiff()));
    }

    private static StatementParamsDto singlePeriodDto() {
        return StatementParamsDto.builder()
                .bookId("book-1")
                .periodType("between")
                .dateRange(new String[]{"2023-01", "2023-01"})
                .subjectCodes(List.of())
                .build();
    }

    private static BookSubject subject(
            String id, String parentId, String code, String name, int level) {
        BookSubject subject = new BookSubject();
        subject.setId(id);
        subject.setParentId(parentId);
        subject.setCode(code);
        subject.setName(name);
        subject.setLevel(level);
        subject.setBookId("book-1");
        return subject;
    }

    private static VoucherItemVo amount(
            String subjectCode, String period, String debit, String credit) {
        VoucherItemVo row = new VoucherItemVo();
        row.setSubjectCode(subjectCode);
        row.setYearPeriod(period);
        row.setDebitAmount(new BigDecimal(debit));
        row.setCreditAmount(new BigDecimal(credit));
        return row;
    }

    private static StatementIncome income(StatementIncomeItem... items) {
        StatementIncome income = new StatementIncome();
        income.setItems(List.of(items));
        return income;
    }

    private static StatementIncomeItem item(String itemCode, String currentBalance) {
        StatementIncomeItem item = new StatementIncomeItem();
        item.setItemCode(itemCode);
        item.setCurrentBalance(new BigDecimal(currentBalance));
        return item;
    }
}
