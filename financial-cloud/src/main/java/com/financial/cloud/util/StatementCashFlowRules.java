package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementCashFlow;
import com.financial.cloud.enums.statement.CashFlowItemEnum;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 现金流量表主表汇总公式与附表倒挤/勾稽。
 */
public final class StatementCashFlowRules {

    public static final BigDecimal RECONCILIATION_TOLERANCE = new BigDecimal("0.01");

    private StatementCashFlowRules() {
    }

    public static Map<String, BigDecimal> calculateSubtotalsAndNetAmounts(
            List<StatementCashFlow> flows, boolean isMonthly, BigDecimal beginBalance) {
        AmountProcessor processor = new AmountProcessor(flows, isMonthly);
        Map<String, BigDecimal> results = new HashMap<>();

        results.put(CashFlowItemEnum.BEGINNING_CASH_BALANCE.getDbCode(), beginBalance);
        results.put(CashFlowItemEnum.BEGINNING_BALANCE_CASH.getDbCode(), beginBalance);

        BigDecimal operatingCashInflow = processor.sumAmounts(CashFlowItemEnum.getOperatingCashInflowDbCodes());
        results.put(CashFlowItemEnum.OPERATING_CASH_INFLOW_SUBTOTAL.getDbCode(), operatingCashInflow);

        BigDecimal operatingCashOutflow = processor.sumAmounts(CashFlowItemEnum.getOperatingCashOutflowDbCodes());
        results.put(CashFlowItemEnum.OPERATING_CASH_OUTFLOW_SUBTOTAL.getDbCode(), operatingCashOutflow);

        BigDecimal operatingCashNet = operatingCashInflow.subtract(operatingCashOutflow);
        results.put(CashFlowItemEnum.OPERATING_CASH_NET.getDbCode(), operatingCashNet);

        BigDecimal investingCashInflow = processor.sumAmounts(CashFlowItemEnum.getInvestingCashInflowDbCodes());
        results.put(CashFlowItemEnum.INVESTING_CASH_INFLOW_SUBTOTAL.getDbCode(), investingCashInflow);

        BigDecimal investingCashOutflow = processor.sumAmounts(CashFlowItemEnum.getInvestingCashOutflowDbCodes());
        results.put(CashFlowItemEnum.INVESTING_CASH_OUTFLOW_SUBTOTAL.getDbCode(), investingCashOutflow);

        BigDecimal investingCashNet = investingCashInflow.subtract(investingCashOutflow);
        results.put(CashFlowItemEnum.INVESTING_CASH_NET.getDbCode(), investingCashNet);

        BigDecimal financingCashInflow = processor.sumAmounts(CashFlowItemEnum.getFinancingCashInflowDbCodes());
        results.put(CashFlowItemEnum.FINANCING_CASH_INFLOW_SUBTOTAL.getDbCode(), financingCashInflow);

        BigDecimal financingCashOutflow = processor.sumAmounts(CashFlowItemEnum.getFinancingCashOutflowDbCodes());
        results.put(CashFlowItemEnum.FINANCING_CASH_OUTFLOW_SUBTOTAL.getDbCode(), financingCashOutflow);

        BigDecimal financingCashNet = financingCashInflow.subtract(financingCashOutflow);
        results.put(CashFlowItemEnum.FINANCING_CASH_NET.getDbCode(), financingCashNet);

        BigDecimal exchangeRateEffect = processor.getAmount(CashFlowItemEnum.EXCHANGE_RATE_EFFECT.getDbCode());

        BigDecimal cashNetIncrease = operatingCashNet
                .add(investingCashNet)
                .add(financingCashNet)
                .add(exchangeRateEffect);
        results.put(CashFlowItemEnum.CASH_NET_INCREASE.getDbCode(), cashNetIncrease);

        BigDecimal endingCashBalance = beginBalance.add(cashNetIncrease);
        results.put(CashFlowItemEnum.ENDING_CASH_BALANCE.getDbCode(), endingCashBalance);

        BigDecimal supplementaryBeforeOther = processor.sumAmounts(
                CashFlowItemEnum.getSupplementaryInformationDbCodes());
        BigDecimal otherValue = operatingCashNet.subtract(supplementaryBeforeOther);
        results.put(CashFlowItemEnum.OTHER_VALUE.getDbCode(), otherValue);

        BigDecimal indirectOperatingNet = supplementaryBeforeOther.add(otherValue);
        results.put(CashFlowItemEnum.OPERATING_CASH_NET_SECOND.getDbCode(), indirectOperatingNet);

        results.put(CashFlowItemEnum.ENDING_BALANCE_CASH.getDbCode(), endingCashBalance);
        BigDecimal endingAmountEqui = processor.getAmount(CashFlowItemEnum.ENDING_BALANCE_CASH_EQUIVALENTS.getDbCode());
        BigDecimal beginningAmountEqui = processor.getAmount(CashFlowItemEnum.BEGINNING_BALANCE_CASH_EQUIVALENTS.getDbCode());
        BigDecimal netIncreaseCash = endingCashBalance
                .subtract(beginBalance)
                .add(endingAmountEqui)
                .subtract(beginningAmountEqui);
        results.put(CashFlowItemEnum.NET_INCREASE_CASH_EQUIVALENTS.getDbCode(), netIncreaseCash);

        return results;
    }

    public static boolean isWithinReconciliationTolerance(BigDecimal directNet, BigDecimal indirectNet) {
        return defaultZero(directNet).subtract(defaultZero(indirectNet)).abs()
                .compareTo(RECONCILIATION_TOLERANCE) <= 0;
    }

    public static BigDecimal reconciliationDiff(BigDecimal directNet, BigDecimal indirectNet) {
        return defaultZero(directNet).subtract(defaultZero(indirectNet));
    }

    static final class AmountProcessor {
        private final Map<String, StatementCashFlow> flowMap;
        private final boolean isMonthly;

        AmountProcessor(List<StatementCashFlow> flows, boolean isMonthly) {
            this.isMonthly = isMonthly;
            this.flowMap = new HashMap<>(flows.size());
            for (StatementCashFlow flow : flows) {
                this.flowMap.put(flow.getItemCode(), flow);
            }
        }

        BigDecimal getAmount(String itemCode) {
            StatementCashFlow flow = flowMap.get(itemCode);
            if (flow == null) {
                return BigDecimal.ZERO;
            }
            BigDecimal amount = isMonthly ? flow.getMonthlyAmount() : flow.getCurrentAmount();
            return amount != null ? amount : BigDecimal.ZERO;
        }

        BigDecimal sumAmounts(List<String> itemCodes) {
            return itemCodes.stream()
                    .map(this::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
