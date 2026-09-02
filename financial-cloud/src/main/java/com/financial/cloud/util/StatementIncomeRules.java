package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementIncomeItem;
import com.financial.cloud.enums.statement.StatementSymbolEnum;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 利润表取数规则与逐级计算公式（营业利润 → 利润总额 → 净利润）。
 */
public final class StatementIncomeRules {

    public static final String PROFIT_AND_LOSS_AMOUNT = "PROFIT_AND_LOSS_AMOUNT";
    public static final String DEBIT_AMOUNT = "DEBIT_AMOUNT";
    public static final String CREDIT_AMOUNT = "CREDIT_AMOUNT";
    public static final BigDecimal FORMULA_TOLERANCE = new BigDecimal("0.01");

    private StatementIncomeRules() {
    }

    /** 利润表逐级公式链偏差（本期 + 累计）。 */
    public static final class FormulaChainDiff {
        private final BigDecimal currentOperatingDiff;
        private final BigDecimal currentTotalDiff;
        private final BigDecimal currentNetDiff;
        private final BigDecimal cumulativeOperatingDiff;
        private final BigDecimal cumulativeTotalDiff;
        private final BigDecimal cumulativeNetDiff;

        FormulaChainDiff(
                BigDecimal currentOperatingDiff,
                BigDecimal currentTotalDiff,
                BigDecimal currentNetDiff,
                BigDecimal cumulativeOperatingDiff,
                BigDecimal cumulativeTotalDiff,
                BigDecimal cumulativeNetDiff) {
            this.currentOperatingDiff = currentOperatingDiff;
            this.currentTotalDiff = currentTotalDiff;
            this.currentNetDiff = currentNetDiff;
            this.cumulativeOperatingDiff = cumulativeOperatingDiff;
            this.cumulativeTotalDiff = cumulativeTotalDiff;
            this.cumulativeNetDiff = cumulativeNetDiff;
        }

        public BigDecimal maxAbsDiff() {
            return currentOperatingDiff.abs()
                    .max(currentTotalDiff.abs())
                    .max(currentNetDiff.abs())
                    .max(cumulativeOperatingDiff.abs())
                    .max(cumulativeTotalDiff.abs())
                    .max(cumulativeNetDiff.abs());
        }

        public boolean withinTolerance() {
            return withinTolerance(FORMULA_TOLERANCE);
        }

        public boolean withinTolerance(BigDecimal tolerance) {
            return maxAbsDiff().compareTo(tolerance) <= 0;
        }
    }

    /**
     * 收入类科目在模板中常误配为 DEBIT_AMOUNT；初始化时纠正为 CREDIT_AMOUNT。
     */
    public static String normalizeIncomeRuleType(String rule, String subjectCode) {
        if (StringUtils.isBlank(rule) || StringUtils.isBlank(subjectCode)) {
            return rule;
        }
        if (!DEBIT_AMOUNT.equalsIgnoreCase(rule)) {
            return rule;
        }
        if (isCreditNatureIncomeSubject(subjectCode)) {
            return CREDIT_AMOUNT;
        }
        return rule;
    }

    /**
     * 聚合取数时：DEBIT_AMOUNT 但本期仅有贷方发生额（收入类）则按贷方取数。
     */
    public static String effectiveAmountRule(String rule, BigDecimal debit, BigDecimal credit) {
        if (!DEBIT_AMOUNT.equalsIgnoreCase(rule)) {
            return rule;
        }
        BigDecimal d = defaultZero(debit).abs();
        BigDecimal c = defaultZero(credit).abs();
        if (d.compareTo(BigDecimal.ZERO) == 0 && c.compareTo(BigDecimal.ZERO) > 0) {
            return CREDIT_AMOUNT;
        }
        return rule;
    }

    static boolean isCreditNatureIncomeSubject(String subjectCode) {
        String code = subjectCode.trim();
        return code.startsWith("5001") || code.startsWith("5051") || code.startsWith("5101")
                || code.startsWith("5111") || code.startsWith("5301")
                || code.startsWith("6001") || code.startsWith("6051") || code.startsWith("6101")
                || code.startsWith("6111") || code.startsWith("6301");
    }

    /**
     * 按规则类型将借贷方发生额归一化为行次金额。
     */
    public static BigDecimal normalizePeriodAmount(BigDecimal debit, BigDecimal credit, String rule) {
        BigDecimal d = defaultZero(debit);
        BigDecimal c = defaultZero(credit);
        if (StringUtils.isBlank(rule) || PROFIT_AND_LOSS_AMOUNT.equalsIgnoreCase(rule)) {
            return d.abs().subtract(c.abs());
        }
        if (DEBIT_AMOUNT.equalsIgnoreCase(rule)) {
            return d;
        }
        if (CREDIT_AMOUNT.equalsIgnoreCase(rule)) {
            return c;
        }
        return d.abs().subtract(c.abs());
    }

    /**
     * 按规则与 symbol 计算对行次的贡献（加项为正、减项为负）。
     */
    public static BigDecimal applyRuleContribution(
            BigDecimal debit, BigDecimal credit, String rule, String symbol) {
        BigDecimal amount = normalizePeriodAmount(debit, credit, rule);
        if (StatementSymbolEnum.MINUS.getValue().equalsIgnoreCase(symbol)) {
            return amount.negate();
        }
        return amount;
    }

    /**
     * 逐级计算营业利润(2)、利润总额(3)、净利润(4)。
     */
    public static void calculateDerivedLines(List<StatementIncomeItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        normalizeSection1AdditionSymbols(items);

        Map<String, StatementIncomeItem> itemsMap = new HashMap<>();
        for (StatementIncomeItem item : items) {
            itemsMap.put(item.getItemCode(), item);
        }

        BigDecimal section1Current = sumSection(items, "1", 3, false);
        BigDecimal section1Year = sumSection(items, "1", 3, true);
        StatementIncomeItem revenueItem = itemsMap.get("1");
        StatementIncomeItem operatingProfitItem = itemsMap.get("2");
        if (revenueItem != null && operatingProfitItem != null) {
            // section1 约定：symbol='+' 为减项，symbol='-' 为加项（如投资收益）
            operatingProfitItem.setCurrentBalance(
                    defaultZero(revenueItem.getCurrentBalance()).subtract(section1Current));
            operatingProfitItem.setCumulativeBalance(
                    defaultZero(revenueItem.getCumulativeBalance()).subtract(section1Year));
        }

        BigDecimal section2Current = sumSection(items, "2", 3, false);
        BigDecimal section2Year = sumSection(items, "2", 3, true);
        StatementIncomeItem totalProfitItem = itemsMap.get("3");
        if (operatingProfitItem != null && totalProfitItem != null) {
            // section2 约定：带符号加总（加项 + / 减项 -）
            totalProfitItem.setCurrentBalance(
                    defaultZero(operatingProfitItem.getCurrentBalance()).add(section2Current));
            totalProfitItem.setCumulativeBalance(
                    defaultZero(operatingProfitItem.getCumulativeBalance()).add(section2Year));
        }

        BigDecimal section3Current = sumSectionExcluding(items, "3", false);
        BigDecimal section3Year = sumSectionExcluding(items, "3", true);
        StatementIncomeItem netProfitItem = itemsMap.get("4");
        if (totalProfitItem != null && netProfitItem != null) {
            // section3 与 section2 同口径：带符号加总（所得税 symbol='-' → 负数）
            netProfitItem.setCurrentBalance(
                    defaultZero(totalProfitItem.getCurrentBalance()).add(section3Current));
            netProfitItem.setCumulativeBalance(
                    defaultZero(totalProfitItem.getCumulativeBalance()).add(section3Year));
        }
    }

    /**
     * section1 用「收入 − 带符号合计」：加项（投资收益等）必须为 '-'，否则会被当成减项。
     */
    static void normalizeSection1AdditionSymbols(List<StatementIncomeItem> items) {
        for (StatementIncomeItem item : items) {
            if (item == null || item.getItemCode() == null) {
                continue;
            }
            String code = item.getItemCode();
            if (code.length() != 3 || !code.startsWith("1")) {
                continue;
            }
            String name = StringUtils.defaultString(item.getItemName());
            if (name.startsWith("加")
                    && StatementSymbolEnum.PLUS.getValue().equalsIgnoreCase(item.getSymbol())) {
                item.setSymbol(StatementSymbolEnum.MINUS.getValue());
            }
        }
    }

    /**
     * 校验营业利润 / 利润总额 / 净利润是否与明细行一致。
     */
    public static FormulaChainDiff computeFormulaChainDiff(List<StatementIncomeItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        normalizeSection1AdditionSymbols(items);
        Map<String, StatementIncomeItem> itemsMap = new HashMap<>();
        for (StatementIncomeItem item : items) {
            itemsMap.put(item.getItemCode(), item);
        }
        if (!itemsMap.containsKey("1") || !itemsMap.containsKey("2")
                || !itemsMap.containsKey("3") || !itemsMap.containsKey("4")) {
            return null;
        }

        BigDecimal currentOperatingDiff = diffOperatingProfit(items, itemsMap, false);
        BigDecimal currentTotalDiff = diffTotalProfit(items, itemsMap, false);
        BigDecimal currentNetDiff = diffNetProfit(items, itemsMap, false);
        BigDecimal cumulativeOperatingDiff = diffOperatingProfit(items, itemsMap, true);
        BigDecimal cumulativeTotalDiff = diffTotalProfit(items, itemsMap, true);
        BigDecimal cumulativeNetDiff = diffNetProfit(items, itemsMap, true);

        return new FormulaChainDiff(
                currentOperatingDiff,
                currentTotalDiff,
                currentNetDiff,
                cumulativeOperatingDiff,
                cumulativeTotalDiff,
                cumulativeNetDiff);
    }

    private static BigDecimal diffOperatingProfit(
            List<StatementIncomeItem> items,
            Map<String, StatementIncomeItem> itemsMap,
            boolean cumulative) {
        BigDecimal revenue = lineAmount(itemsMap.get("1"), cumulative);
        BigDecimal section1 = sumSection(items, "1", 3, cumulative);
        BigDecimal expected = revenue.subtract(section1);
        BigDecimal actual = lineAmount(itemsMap.get("2"), cumulative);
        return actual.subtract(expected);
    }

    private static BigDecimal diffTotalProfit(
            List<StatementIncomeItem> items,
            Map<String, StatementIncomeItem> itemsMap,
            boolean cumulative) {
        BigDecimal operating = lineAmount(itemsMap.get("2"), cumulative);
        BigDecimal section2 = sumSection(items, "2", 3, cumulative);
        BigDecimal expected = operating.add(section2);
        BigDecimal actual = lineAmount(itemsMap.get("3"), cumulative);
        return actual.subtract(expected);
    }

    private static BigDecimal diffNetProfit(
            List<StatementIncomeItem> items,
            Map<String, StatementIncomeItem> itemsMap,
            boolean cumulative) {
        BigDecimal total = lineAmount(itemsMap.get("3"), cumulative);
        BigDecimal section3 = sumSectionExcluding(items, "3", cumulative);
        BigDecimal expected = total.add(section3);
        BigDecimal actual = lineAmount(itemsMap.get("4"), cumulative);
        return actual.subtract(expected);
    }

    private static BigDecimal lineAmount(StatementIncomeItem item, boolean cumulative) {
        if (item == null) {
            return BigDecimal.ZERO;
        }
        return cumulative ? defaultZero(item.getCumulativeBalance()) : defaultZero(item.getCurrentBalance());
    }

    static BigDecimal sumSection(List<StatementIncomeItem> items, String prefix, int codeLength, boolean cumulative) {
        BigDecimal total = BigDecimal.ZERO;
        for (StatementIncomeItem item : items) {
            String code = item.getItemCode();
            if (code == null || code.length() != codeLength || !code.startsWith(prefix)) {
                continue;
            }
            total = total.add(signedAmount(item, cumulative));
        }
        return total;
    }

    static BigDecimal sumSectionExcluding(
            List<StatementIncomeItem> items, String excludeCode, boolean cumulative) {
        BigDecimal total = BigDecimal.ZERO;
        for (StatementIncomeItem item : items) {
            String code = item.getItemCode();
            if (code == null || excludeCode.equalsIgnoreCase(code) || !code.startsWith(excludeCode)) {
                continue;
            }
            total = total.add(signedAmount(item, cumulative));
        }
        return total;
    }

    private static BigDecimal signedAmount(StatementIncomeItem item, boolean cumulative) {
        BigDecimal val = cumulative
                ? defaultZero(item.getCumulativeBalance())
                : defaultZero(item.getCurrentBalance());
        if (StatementSymbolEnum.MINUS.getValue().equalsIgnoreCase(item.getSymbol())) {
            return val.negate();
        }
        return val;
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
