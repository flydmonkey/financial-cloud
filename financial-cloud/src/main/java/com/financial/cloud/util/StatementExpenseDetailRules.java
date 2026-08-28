package com.financial.cloud.util;

import com.financial.cloud.dto.statement.ExpenseReconciliationWarning;
import com.financial.cloud.dto.statement.StatementExpenseDetailItem;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 费用明细表纯函数：年度标签、rollup、合计、零行过滤、层级截断、利润表勾稽。
 */
public final class StatementExpenseDetailRules {

    public static final String YEAR_TOTAL_KEY = "yearTotal";

    private StatementExpenseDetailRules() {
    }

    public static String yearLabel(List<String> periods) {
        if (periods == null || periods.isEmpty()) {
            return "区间合计";
        }
        Set<String> years = new HashSet<>();
        for (String period : periods) {
            if (StringUtils.isNotBlank(period) && period.length() >= 4) {
                years.add(period.substring(0, 4));
            }
        }
        if (years.size() == 1) {
            return years.iterator().next() + "年合计";
        }
        return "区间合计";
    }

    public static BigDecimal sumAmounts(Map<String, BigDecimal> amounts, List<String> periods) {
        BigDecimal total = BigDecimal.ZERO;
        if (periods == null || periods.isEmpty()) {
            return total;
        }
        for (String period : periods) {
            total = total.add(defaultZero(amounts == null ? null : amounts.get(period)));
        }
        return total;
    }

    public static void rollup(StatementExpenseDetailItem node, List<String> periods) {
        if (node == null) {
            return;
        }
        List<StatementExpenseDetailItem> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            if (node.getAmounts() == null) {
                node.setAmounts(new HashMap<>());
            }
            node.setYearTotal(sumAmounts(node.getAmounts(), periods));
            return;
        }
        for (StatementExpenseDetailItem child : children) {
            rollup(child, periods);
        }
        Map<String, BigDecimal> rolled = new HashMap<>();
        BigDecimal yearTotal = BigDecimal.ZERO;
        for (String period : periods) {
            BigDecimal periodTotal = BigDecimal.ZERO;
            for (StatementExpenseDetailItem child : children) {
                Map<String, BigDecimal> childAmounts = child.getAmounts();
                periodTotal = periodTotal.add(defaultZero(childAmounts == null ? null : childAmounts.get(period)));
            }
            rolled.put(period, periodTotal);
        }
        for (StatementExpenseDetailItem child : children) {
            yearTotal = yearTotal.add(defaultZero(child.getYearTotal()));
        }
        node.setAmounts(rolled);
        node.setYearTotal(yearTotal);
    }

    public static Map<String, BigDecimal> computeTotals(
            List<StatementExpenseDetailItem> roots, List<String> periods) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        if (periods != null) {
            for (String period : periods) {
                totals.put(period, BigDecimal.ZERO);
            }
        }
        totals.put(YEAR_TOTAL_KEY, BigDecimal.ZERO);
        if (roots == null || roots.isEmpty()) {
            return totals;
        }
        for (StatementExpenseDetailItem root : roots) {
            if (root == null) {
                continue;
            }
            Map<String, BigDecimal> amounts = root.getAmounts();
            if (periods != null) {
                for (String period : periods) {
                    totals.merge(period, defaultZero(amounts == null ? null : amounts.get(period)), BigDecimal::add);
                }
            }
            totals.merge(YEAR_TOTAL_KEY, defaultZero(root.getYearTotal()), BigDecimal::add);
        }
        return totals;
    }

    public static void filterZeroLeaves(List<StatementExpenseDetailItem> roots, List<String> periods) {
        if (roots == null) {
            return;
        }
        for (StatementExpenseDetailItem root : roots) {
            filterZeroLeavesNode(root, periods);
        }
    }

    public static List<StatementExpenseDetailItem> truncateLevel(
            List<StatementExpenseDetailItem> roots, int maxLevel) {
        if (roots == null || maxLevel <= 0) {
            return roots;
        }
        for (StatementExpenseDetailItem root : roots) {
            truncateLevelNode(root, maxLevel);
        }
        return roots;
    }

    public static List<ExpenseReconciliationWarning> reconcile(
            Map<String, Map<String, BigDecimal>> detailByCodePeriod,
            Map<String, Map<String, BigDecimal>> incomeByCodePeriod,
            BigDecimal tolerance) {
        List<ExpenseReconciliationWarning> warnings = new ArrayList<>();
        if (detailByCodePeriod == null || incomeByCodePeriod == null) {
            return warnings;
        }
        BigDecimal allowedDiff = defaultZero(tolerance);
        for (Map.Entry<String, Map<String, BigDecimal>> detailEntry : detailByCodePeriod.entrySet()) {
            String code = detailEntry.getKey();
            Map<String, BigDecimal> incomePeriods = incomeByCodePeriod.get(code);
            if (incomePeriods == null) {
                continue;
            }
            Map<String, BigDecimal> detailPeriods = detailEntry.getValue();
            if (detailPeriods == null) {
                continue;
            }
            for (Map.Entry<String, BigDecimal> periodEntry : detailPeriods.entrySet()) {
                String period = periodEntry.getKey();
                if (!incomePeriods.containsKey(period)) {
                    continue;
                }
                BigDecimal detailAmount = defaultZero(periodEntry.getValue());
                BigDecimal incomeAmount = defaultZero(incomePeriods.get(period));
                BigDecimal diff = detailAmount.subtract(incomeAmount).abs();
                if (diff.compareTo(allowedDiff) > 0) {
                    warnings.add(ExpenseReconciliationWarning.builder()
                            .subjectCode(code)
                            .period(period)
                            .detailAmount(detailAmount)
                            .incomeAmount(incomeAmount)
                            .diff(diff)
                            .build());
                }
            }
        }
        return warnings;
    }

    private static void filterZeroLeavesNode(StatementExpenseDetailItem node, List<String> periods) {
        if (node == null) {
            return;
        }
        List<StatementExpenseDetailItem> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            Iterator<StatementExpenseDetailItem> iterator = children.iterator();
            while (iterator.hasNext()) {
                StatementExpenseDetailItem child = iterator.next();
                filterZeroLeavesNode(child, periods);
                if (isZeroLeaf(child, periods)) {
                    iterator.remove();
                }
            }
        }
    }

    public static boolean isZeroLeaf(StatementExpenseDetailItem node, List<String> periods) {
        if (node == null) {
            return false;
        }
        List<StatementExpenseDetailItem> children = node.getChildren();
        boolean noChildren = children == null || children.isEmpty();
        if (!noChildren || defaultZero(node.getYearTotal()).compareTo(BigDecimal.ZERO) != 0) {
            return false;
        }
        Map<String, BigDecimal> amounts = node.getAmounts();
        if (periods != null) {
            for (String period : periods) {
                if (defaultZero(amounts == null ? null : amounts.get(period))
                        .compareTo(BigDecimal.ZERO) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void truncateLevelNode(StatementExpenseDetailItem node, int maxLevel) {
        if (node == null) {
            return;
        }
        Integer level = node.getLevel();
        if (level != null && level >= maxLevel) {
            node.setChildren(new ArrayList<>());
            return;
        }
        List<StatementExpenseDetailItem> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }
        for (StatementExpenseDetailItem child : children) {
            truncateLevelNode(child, maxLevel);
        }
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
