package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.domain.statement.StatementRules;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementDrillVo;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.statement.StatementSymbolEnum;
import com.financial.cloud.enums.statement.StatementTypeEnum;
import com.financial.cloud.repository.statement.StatementRulesMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.util.StatementBalanceSheetRules;
import com.financial.cloud.util.StatementIncomeRules;
import com.financial.cloud.util.SubjectCodeCompat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报表数字下钻：从报表行次追溯到构成它的科目及金额。
 * 取数口径与报表生成完全同源——
 * 资产负债表走结账快照 {@code statement_subject_balance}（同 refreshItemsBalance），
 * 利润表走过账凭证分录聚合（同 accumulateLineAmount），保证下钻合计与报表数字一致。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class StatementDrillService {

    private final StatementRulesMapper rulesMapper;
    private final StatementSubjectBalanceMapper subjectBalanceMapper;
    private final VoucherItemMapper voucherItemMapper;

    /**
     * 资产负债表行下钻（期末余额口径）。
     *
     * @param dto      查询参数（periodType + reportDate，parse 后取期末月快照）
     * @param itemCode 报表行次编码
     */
    public StatementDrillVo drillBalanceSheet(StatementParamsDto dto, String itemCode) {
        dto.parse();
        String bookId = dto.getBookId();
        // 区间报表（季/半年/年）的期末余额取最后一个账期的快照
        String yearPeriod = dto.getReportDate();

        List<StatementRules> rules = selectRules(bookId, StatementTypeEnum.balance_sheet.name(), itemCode);
        StatementDrillVo result = StatementDrillVo.builder()
                .type(StatementTypeEnum.balance_sheet.name())
                .itemCode(itemCode)
                .yearPeriod(yearPeriod)
                .total(BigDecimal.ZERO)
                .subjects(new ArrayList<>())
                .build();
        if (rules.isEmpty()) {
            return result;
        }

        Set<String> subjectCodes = SubjectCodeCompat.expandLookupCodes(
                rules.stream().map(StatementRules::getSubjectCode).toList());
        if (CollectionUtils.isEmpty(subjectCodes)) {
            return result;
        }

        LambdaQueryWrapper<StatementSubjectBalance> lqw = Wrappers.lambdaQuery();
        lqw.in(StatementSubjectBalance::getSubjectCode, subjectCodes);
        lqw.eq(StatementSubjectBalance::getBookId, bookId);
        lqw.eq(StatementSubjectBalance::getYearPeriod, yearPeriod);
        List<StatementSubjectBalance> subjectBalances = subjectBalanceMapper.selectList(lqw);
        Map<String, List<StatementSubjectBalance>> subjectMapByCode = subjectBalances.stream()
                .collect(Collectors.groupingBy(StatementSubjectBalance::getSubjectCode));

        // 按科目编码聚合（同一科目可能被多条规则引用，合并为一行展示）
        Map<String, StatementDrillVo.SubjectLine> lineBySubject = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (StatementRules rule : rules) {
            boolean minus = StatementSymbolEnum.MINUS.getValue().equals(rule.getSymbol());
            for (String candidate : SubjectCodeCompat.lookupCandidates(rule.getSubjectCode())) {
                for (StatementSubjectBalance balance :
                        subjectMapByCode.getOrDefault(candidate, List.of())) {
                    BigDecimal amount = StatementBalanceSheetRules.normalizeClosingBalance(balance, rule.getRule());
                    BigDecimal signed = minus ? amount.negate() : amount;
                    total = total.add(signed);

                    StatementDrillVo.SubjectLine line = lineBySubject.computeIfAbsent(
                            balance.getSubjectCode(),
                            code -> StatementDrillVo.SubjectLine.builder()
                                    .subjectCode(code)
                                    .subjectName(balance.getSubjectName())
                                    .direction(balance.getDirection())
                                    .rule(rule.getRule())
                                    .symbol(rule.getSymbol())
                                    .debit(BigDecimal.ZERO)
                                    .credit(BigDecimal.ZERO)
                                    .amount(BigDecimal.ZERO)
                                    .build());
                    line.setDebit(defaultZero(line.getDebit()).add(defaultZero(balance.getClosingBalanceDebit())));
                    line.setCredit(defaultZero(line.getCredit()).add(defaultZero(balance.getClosingBalanceCredit())));
                    line.setAmount(line.getAmount().add(signed));
                }
            }
        }
        result.setSubjects(new ArrayList<>(lineBySubject.values()));
        result.setTotal(total);
        return result;
    }

    /**
     * 利润表行下钻（本期发生额口径，仅已过账凭证）。
     *
     * @param dto      查询参数（periodType + reportDate）
     * @param itemCode 报表行次编码
     */
    public StatementDrillVo drillIncome(StatementParamsDto dto, String itemCode) {
        dto.parse();
        dto.setPostedOnly(true);

        List<StatementRules> rules = selectRules(dto.getBookId(), StatementTypeEnum.income.name(), itemCode);
        StatementDrillVo result = StatementDrillVo.builder()
                .type(StatementTypeEnum.income.name())
                .itemCode(itemCode)
                .yearPeriod(dto.getReportDate())
                .total(BigDecimal.ZERO)
                .subjects(new ArrayList<>())
                .build();
        if (rules.isEmpty()) {
            return result;
        }

        List<VoucherItemVo> voucherItemVos = voucherItemMapper.selectSubjectAmount(dto);

        // 与 accumulateLineAmount 同口径：多个子科目规则映射到同一账套科目时只计一次
        Set<String> appliedSubjects = new HashSet<>();
        Map<String, StatementDrillVo.SubjectLine> lineBySubject = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (StatementRules rule : rules) {
            for (VoucherItemVo voucherItem : voucherItemVos) {
                if (!SubjectCodeCompat.incomeRuleMatchesVoucherSubject(
                        rule.getSubjectCode(), voucherItem.getSubjectCode())) {
                    continue;
                }
                if (!appliedSubjects.add(voucherItem.getSubjectCode())) {
                    continue;
                }
                String effectiveRule = StatementIncomeRules.effectiveAmountRule(
                        rule.getRule(),
                        voucherItem.getDebitAmount(),
                        voucherItem.getCreditAmount());
                BigDecimal contribution = StatementIncomeRules.applyRuleContribution(
                        voucherItem.getDebitAmount(),
                        voucherItem.getCreditAmount(),
                        effectiveRule,
                        rule.getSymbol());
                total = total.add(contribution);

                lineBySubject.computeIfAbsent(
                        voucherItem.getSubjectCode(),
                        code -> StatementDrillVo.SubjectLine.builder()
                                .subjectCode(code)
                                .subjectName(voucherItem.getSubjectName())
                                .rule(effectiveRule)
                                .symbol(rule.getSymbol())
                                .debit(defaultZero(voucherItem.getDebitAmount()))
                                .credit(defaultZero(voucherItem.getCreditAmount()))
                                .amount(contribution)
                                .build());
            }
        }
        result.setSubjects(new ArrayList<>(lineBySubject.values()));
        result.setTotal(total);
        return result;
    }

    private List<StatementRules> selectRules(String bookId, String type, String itemCode) {
        LambdaQueryWrapper<StatementRules> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementRules::getBookId, bookId);
        lqw.eq(StatementRules::getType, type);
        lqw.eq(StatementRules::getItemCode, itemCode);
        return rulesMapper.selectList(lqw);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
