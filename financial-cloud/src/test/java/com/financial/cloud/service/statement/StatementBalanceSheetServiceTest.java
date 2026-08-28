package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.financial.cloud.domain.statement.StatementBalanceSheetItem;
import com.financial.cloud.domain.statement.StatementRules;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementBalanceSheetItemListVo;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.enums.statement.AssetOrLiabilityEnum;
import com.financial.cloud.enums.statement.StatementSymbolEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.standard.StandardStatementBalanceSheetMapper;
import com.financial.cloud.repository.standard.StandardStatementRulesMapper;
import com.financial.cloud.repository.statement.StatementBalanceSheetItemMapper;
import com.financial.cloud.repository.statement.StatementBalanceSheetMapper;
import com.financial.cloud.repository.statement.StatementRulesMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class StatementBalanceSheetServiceTest {

    @Mock
    private ConfigSysService configSysService;
    @Mock
    private StatementBalanceSheetMapper balanceSheetMapper;
    @Mock
    private StatementBalanceSheetItemMapper balanceSheetItemMapper;
    @Mock
    private IdentifierGenerator identifierGenerator;
    @Mock
    private StatementRulesMapper statementRulesMapper;
    @Mock
    private StandardStatementBalanceSheetMapper standardStatementBalanceSheetMapper;
    @Mock
    private StandardStatementRulesMapper standardStatementRulesMapper;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private StatementRulesMapper rulesMapper;
    @Mock
    private StatementSubjectBalanceMapper subjectBalanceMapper;

    @InjectMocks
    private StatementBalanceSheetService statementBalanceSheetService;

    @Test
    void updateRuleBalance_debitBalanceRule_accumulatesMultipleRows() {
        StatementRules arRule = StatementRules.builder()
                .rule(com.financial.cloud.util.StatementBalanceSheetRules.DEBIT_BALANCE)
                .build();
        StatementSubjectBalance debitRow = StatementSubjectBalance.builder()
                .direction(SubjectDirectionEnum.DEBIT.getValue())
                .closingBalanceDebit(bd("10000"))
                .closingBalanceCredit(BigDecimal.ZERO)
                .balance(bd("10000"))
                .build();
        StatementSubjectBalance creditRow = StatementSubjectBalance.builder()
                .direction(SubjectDirectionEnum.DEBIT.getValue())
                .closingBalanceDebit(bd("0"))
                .closingBalanceCredit(bd("3000"))
                .balance(bd("-3000"))
                .build();

        statementBalanceSheetService.updateRuleBalance(debitRow, arRule);
        statementBalanceSheetService.updateRuleBalance(creditRow, arRule);

        assertEquals(0, bd("10000").compareTo(arRule.getClosingBalance()));
    }

    @Test
    void updateRuleBalance_creditBalanceRule_accumulatesMultipleRows() {
        StatementRules advanceRule = StatementRules.builder()
                .rule(com.financial.cloud.util.StatementBalanceSheetRules.CREDIT_BALANCE)
                .build();
        StatementSubjectBalance creditRow = StatementSubjectBalance.builder()
                .direction(SubjectDirectionEnum.DEBIT.getValue())
                .closingBalanceDebit(BigDecimal.ZERO)
                .closingBalanceCredit(bd("3000"))
                .balance(bd("-3000"))
                .build();

        statementBalanceSheetService.updateRuleBalance(creditRow, advanceRule);

        assertEquals(0, bd("3000").compareTo(advanceRule.getClosingBalance()));
    }

    @Test
    void insertSubtotals_balancedSeed_keepsAssetsEqualToLiabilities() {
        List<StatementBalanceSheetItem> seed = StatementBalanceSheetSeed.balancedItems();

        StatementBalanceSheetItemListVo result = statementBalanceSheetService.insertSubtotals(seed);

        assertNotNull(result);
        assertNotNull(result.getAssets());
        assertNotNull(result.getLiability());

        BigDecimal assetTotal = grandTotal(result.getAssets());
        BigDecimal liabilityTotal = grandTotal(result.getLiability());

        assertEquals(0, assetTotal.compareTo(liabilityTotal));
        assertEquals(0, new BigDecimal("8000").compareTo(assetTotal));
    }

    @Test
    void normalizeByDirection_creditAccountUsesClosingCreditBalance() {
        StatementSubjectBalance subjectBalance = StatementSubjectBalance.builder()
                .direction(SubjectDirectionEnum.CREDIT.getValue())
                .balance(new BigDecimal("-172608.60"))
                .closingBalanceDebit(BigDecimal.ZERO)
                .closingBalanceCredit(new BigDecimal("172608.60"))
                .build();

        BigDecimal normalized = StatementBalanceSheetService.normalizeByDirection(
                subjectBalance.getDirection(),
                subjectBalance.getClosingBalanceDebit(),
                subjectBalance.getClosingBalanceCredit(),
                subjectBalance.getBalance());

        assertEquals(0, new BigDecimal("172608.60").compareTo(normalized));
    }

    @Test
    void normalizeByDirection_debitAccountUsesDebitMinusCredit() {
        BigDecimal normalized = StatementBalanceSheetService.normalizeByDirection(
                SubjectDirectionEnum.DEBIT.getValue(),
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                new BigDecimal("100000"));

        assertEquals(0, new BigDecimal("100000").compareTo(normalized));
    }

    @Test
    void computeGrandTotalDiff_returnsAssetMinusLiability() {
        StatementBalanceSheetItemListVo vo = new StatementBalanceSheetItemListVo();
        vo.setAssets(List.of(grandItem(AssetOrLiabilityEnum.asset.name(), "1199", 1, bd("120000"))));
        vo.setLiability(List.of(grandItem(AssetOrLiabilityEnum.liability.name(), "2299", 1, bd("100000"))));

        StatementBalanceSheetService.GrandTotalDiff diff =
                StatementBalanceSheetService.computeGrandTotalDiff(vo);

        assertNotNull(diff);
        assertEquals(0, bd("120000").compareTo(diff.assetTotal()));
        assertEquals(0, bd("100000").compareTo(diff.liabilityTotal()));
        assertEquals(0, bd("20000").compareTo(diff.diff()));
        assertFalse(diff.withinTolerance());
    }

    @Test
    void computeGrandTotalDiff_withinToleranceWhenDiffIsOneCent() {
        StatementBalanceSheetItemListVo vo = new StatementBalanceSheetItemListVo();
        vo.setAssets(List.of(grandItem(AssetOrLiabilityEnum.asset.name(), "1199", 1, bd("100000.00"))));
        vo.setLiability(List.of(grandItem(AssetOrLiabilityEnum.liability.name(), "2299", 1, bd("99999.99"))));

        StatementBalanceSheetService.GrandTotalDiff diff =
                StatementBalanceSheetService.computeGrandTotalDiff(vo);

        assertNotNull(diff);
        assertTrue(diff.withinTolerance());
    }

    @Test
    void reconcileGrandTotals_adjustsLiabilityWhenOutOfBalance() {
        StatementBalanceSheetItem liabilityGrand =
                grandItem(AssetOrLiabilityEnum.liability.name(), "2299", 1, bd("90000"));
        StatementBalanceSheetItemListVo vo = new StatementBalanceSheetItemListVo();
        vo.setAssets(List.of(grandItem(AssetOrLiabilityEnum.asset.name(), "1199", 1, bd("100000"))));
        vo.setLiability(List.of(liabilityGrand));

        statementBalanceSheetService.reconcileGrandTotals(vo);

        assertEquals(0, bd("100000").compareTo(liabilityGrand.getCurrentBalance()));
    }

    @Test
    void reconcileGrandTotals_keepsBalancedTotalsUnchanged() {
        StatementBalanceSheetItem liabilityGrand =
                grandItem(AssetOrLiabilityEnum.liability.name(), "2299", 1, bd("100000"));
        StatementBalanceSheetItemListVo vo = new StatementBalanceSheetItemListVo();
        vo.setAssets(List.of(grandItem(AssetOrLiabilityEnum.asset.name(), "1199", 1, bd("100000"))));
        vo.setLiability(List.of(liabilityGrand));

        statementBalanceSheetService.reconcileGrandTotals(vo);

        assertEquals(0, bd("100000").compareTo(liabilityGrand.getCurrentBalance()));
    }

    @Test
    void reconcileGrandTotals_strictModeThrowsWhenOutOfBalance() {
        ReflectionTestUtils.setField(statementBalanceSheetService, "strictTrialBalance", true);
        StatementBalanceSheetItemListVo vo = new StatementBalanceSheetItemListVo();
        vo.setAssets(List.of(grandItem(AssetOrLiabilityEnum.asset.name(), "1199", 1, bd("100000"))));
        vo.setLiability(List.of(grandItem(AssetOrLiabilityEnum.liability.name(), "2299", 1, bd("90000"))));

        ServiceException ex = assertThrows(
                ServiceException.class,
                () -> statementBalanceSheetService.reconcileGrandTotals(vo));

        assertEquals(StatementErrorCode.BALANCE_SHEET_TRIAL_BALANCE_FAILED.getCode(), ex.getCode());
        ReflectionTestUtils.setField(statementBalanceSheetService, "strictTrialBalance", false);
    }

    @Test
    void updateRuleBalance_accumulatesClosingAndOpeningBalances() {
        StatementRules rule = rule("book-test", "1101", "1001", StatementSymbolEnum.PLUS.getValue());
        StatementSubjectBalance cashOnHand = subjectBalance(
                "book-test", "2026-08", "1001", SubjectDirectionEnum.DEBIT.getValue(),
                bd("10000"), bd("10000"), bd("10000"));

        statementBalanceSheetService.updateRuleBalance(cashOnHand, rule);

        assertEquals(0, bd("10000").compareTo(rule.getClosingBalance()));
        assertEquals(0, bd("10000").compareTo(rule.getOpeningYearBalance()));
    }

    @Test
    void updateRuleBalance_mergesMultipleSubjectsIntoReportLine() {
        StatementBalanceSheetItem monetary = sheetItem("1101", bd("0"), bd("0"));
        StatementRules rule1001 = rule("book-test", "1101", "1001", StatementSymbolEnum.PLUS.getValue());
        StatementRules rule1002 = rule("book-test", "1101", "1002", StatementSymbolEnum.PLUS.getValue());

        statementBalanceSheetService.updateRuleBalance(
                subjectBalance("book-test", "2026-08", "1001", SubjectDirectionEnum.DEBIT.getValue(),
                        bd("10000"), bd("10000"), bd("10000")),
                rule1001);
        statementBalanceSheetService.updateRuleBalance(
                subjectBalance("book-test", "2026-08", "1002", SubjectDirectionEnum.DEBIT.getValue(),
                        bd("90000"), bd("90000"), bd("90000")),
                rule1002);

        monetary.setCurrentBalance(
                monetary.getCurrentBalance().add(rule1001.getClosingBalance()).add(rule1002.getClosingBalance()));
        monetary.setInitialBalance(
                monetary.getInitialBalance().add(rule1001.getOpeningYearBalance()).add(rule1002.getOpeningYearBalance()));

        assertEquals(0, bd("100000").compareTo(monetary.getCurrentBalance()));
        assertEquals(0, bd("100000").compareTo(monetary.getInitialBalance()));
    }

    @Test
    void computeGrandTotalDiff_returnsNullWhenGrandTotalsMissing() {
        assertNull(StatementBalanceSheetService.computeGrandTotalDiff(null));
        assertNull(StatementBalanceSheetService.computeGrandTotalDiff(new StatementBalanceSheetItemListVo()));
    }

    private static StatementBalanceSheetItem grandItem(String side, String code, int level, BigDecimal current) {
        return StatementBalanceSheetItem.builder()
                .assetOrLiability(side)
                .itemCode(code)
                .itemName("总计")
                .level(level)
                .currentBalance(current)
                .initialBalance(current)
                .build();
    }

    private static StatementBalanceSheetItem sheetItem(String code, BigDecimal current, BigDecimal initial) {
        return StatementBalanceSheetItem.builder()
                .itemCode(code)
                .itemName("货币资金")
                .currentBalance(current)
                .initialBalance(initial)
                .build();
    }

    private static StatementRules rule(String bookId, String itemCode, String subjectCode, String symbol) {
        return StatementRules.builder()
                .bookId(bookId)
                .itemCode(itemCode)
                .subjectCode(subjectCode)
                .symbol(symbol)
                .rule("BALANCE")
                .type("balance_sheet")
                .build();
    }

    private static StatementSubjectBalance subjectBalance(
            String bookId,
            String period,
            String subjectCode,
            String direction,
            BigDecimal balance,
            BigDecimal closingDebit,
            BigDecimal openingDebit) {
        return StatementSubjectBalance.builder()
                .bookId(bookId)
                .yearPeriod(period)
                .subjectCode(subjectCode)
                .direction(direction)
                .balance(balance)
                .closingBalanceDebit(closingDebit)
                .closingBalanceCredit(BigDecimal.ZERO)
                .openingYearBalanceDebit(openingDebit)
                .openingYearBalanceCredit(BigDecimal.ZERO)
                .build();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal grandTotal(List<StatementBalanceSheetItem> items) {
        return items.stream()
                .filter(item -> item.getItemCode() != null && item.getItemCode().endsWith("99"))
                .max(Comparator.comparing(StatementBalanceSheetItem::getItemCode))
                .map(StatementBalanceSheetItem::getCurrentBalance)
                .orElse(BigDecimal.ZERO);
    }
}
