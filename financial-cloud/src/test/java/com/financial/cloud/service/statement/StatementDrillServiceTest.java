package com.financial.cloud.service.statement;

import com.financial.cloud.domain.statement.StatementRules;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementDrillVo;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.repository.statement.StatementRulesMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementDrillServiceTest {

    @Mock
    private StatementRulesMapper rulesMapper;
    @Mock
    private StatementSubjectBalanceMapper subjectBalanceMapper;
    @Mock
    private VoucherItemMapper voucherItemMapper;

    @InjectMocks
    private StatementDrillService drillService;

    private StatementRules rule(String type, String itemCode, String subjectCode, String rule, String symbol) {
        return StatementRules.builder()
                .bookId("book-1")
                .type(type)
                .itemCode(itemCode)
                .subjectCode(subjectCode)
                .rule(rule)
                .symbol(symbol)
                .build();
    }

    private StatementParamsDto monthDto() {
        StatementParamsDto dto = new StatementParamsDto();
        dto.setBookId("book-1");
        dto.setPeriodType("month");
        dto.setReportDate("2026-08");
        return dto;
    }

    @Test
    void balanceSheetDrillAggregatesSubjectsWithSymbol() {
        when(rulesMapper.selectList(any())).thenReturn(List.of(
                rule("balance_sheet", "1101", "1001", "BALANCE", "+"),
                rule("balance_sheet", "1101", "1002", "BALANCE", "-")));
        StatementSubjectBalance cash = StatementSubjectBalance.builder()
                .bookId("book-1").yearPeriod("2026-08")
                .subjectCode("1001").subjectName("库存现金").direction("1")
                .closingBalanceDebit(new BigDecimal("5000"))
                .closingBalanceCredit(BigDecimal.ZERO)
                .balance(new BigDecimal("5000"))
                .build();
        StatementSubjectBalance bank = StatementSubjectBalance.builder()
                .bookId("book-1").yearPeriod("2026-08")
                .subjectCode("1002").subjectName("银行存款").direction("1")
                .closingBalanceDebit(new BigDecimal("3000"))
                .closingBalanceCredit(BigDecimal.ZERO)
                .balance(new BigDecimal("3000"))
                .build();
        when(subjectBalanceMapper.selectList(any())).thenReturn(List.of(cash, bank));

        StatementDrillVo vo = drillService.drillBalanceSheet(monthDto(), "1101");

        assertEquals("balance_sheet", vo.getType());
        assertEquals("2026-08", vo.getYearPeriod());
        assertEquals(2, vo.getSubjects().size());
        // 5000 (+) - 3000 (-)
        assertEquals(0, new BigDecimal("2000").compareTo(vo.getTotal()));
        StatementDrillVo.SubjectLine bankLine = vo.getSubjects().stream()
                .filter(s -> "1002".equals(s.getSubjectCode())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("-3000").compareTo(bankLine.getAmount()));
    }

    @Test
    void balanceSheetDrillNormalizesCreditDirection() {
        when(rulesMapper.selectList(any())).thenReturn(List.of(
                rule("balance_sheet", "2101", "2202", "BALANCE", "+")));
        StatementSubjectBalance payable = StatementSubjectBalance.builder()
                .bookId("book-1").yearPeriod("2026-08")
                .subjectCode("2202").subjectName("应付账款").direction("2")
                .closingBalanceDebit(new BigDecimal("100"))
                .closingBalanceCredit(new BigDecimal("900"))
                .balance(new BigDecimal("800"))
                .build();
        when(subjectBalanceMapper.selectList(any())).thenReturn(List.of(payable));

        StatementDrillVo vo = drillService.drillBalanceSheet(monthDto(), "2101");

        // 贷方科目：贷 − 借 = 800
        assertEquals(0, new BigDecimal("800").compareTo(vo.getTotal()));
        assertEquals("2202", vo.getSubjects().get(0).getSubjectCode());
    }

    @Test
    void balanceSheetDrillReturnsEmptyWhenNoRules() {
        when(rulesMapper.selectList(any())).thenReturn(List.of());

        StatementDrillVo vo = drillService.drillBalanceSheet(monthDto(), "9999");

        assertTrue(vo.getSubjects().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotal()));
    }

    @Test
    void incomeDrillUsesPostedVoucherAmountsWithDedupe() {
        when(rulesMapper.selectList(any())).thenReturn(List.of(
                rule("income", "1", "660201", "DEBIT_AMOUNT", "-"),
                rule("income", "1", "660202", "DEBIT_AMOUNT", "-")));
        // 两条规则都映射到账套科目 5602（小企业准则），应只计一次
        VoucherItemVo item = new VoucherItemVo();
        item.setSubjectCode("5602");
        item.setSubjectName("管理费用");
        item.setDebitAmount(new BigDecimal("1200"));
        item.setCreditAmount(BigDecimal.ZERO);
        when(voucherItemMapper.selectSubjectAmount(any())).thenReturn(List.of(item));

        StatementDrillVo vo = drillService.drillIncome(monthDto(), "1");

        assertEquals(1, vo.getSubjects().size());
        // symbol '-' → 负贡献，且只计一次
        assertEquals(0, new BigDecimal("-1200").compareTo(vo.getTotal()));
        assertEquals("5602", vo.getSubjects().get(0).getSubjectCode());
    }

    @Test
    void incomeDrillAppliesEffectiveCreditRuleForIncomeSubjects() {
        when(rulesMapper.selectList(any())).thenReturn(List.of(
                rule("income", "101", "6001", "DEBIT_AMOUNT", "+")));
        // 规则误配为 DEBIT_AMOUNT，但本期仅有贷方发生额 → 按贷方取数
        VoucherItemVo item = new VoucherItemVo();
        item.setSubjectCode("5001");
        item.setSubjectName("主营业务收入");
        item.setDebitAmount(BigDecimal.ZERO);
        item.setCreditAmount(new BigDecimal("8800"));
        when(voucherItemMapper.selectSubjectAmount(any())).thenReturn(List.of(item));

        StatementDrillVo vo = drillService.drillIncome(monthDto(), "101");

        assertEquals(0, new BigDecimal("8800").compareTo(vo.getTotal()));
        assertEquals("CREDIT_AMOUNT", vo.getSubjects().get(0).getRule());
    }

    @Test
    void incomeDrillReturnsEmptyWhenNoRules() {
        when(rulesMapper.selectList(any())).thenReturn(List.of());

        StatementDrillVo vo = drillService.drillIncome(monthDto(), "404");

        assertTrue(vo.getSubjects().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotal()));
    }
}
