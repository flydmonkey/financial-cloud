package com.financial.cloud.service.hr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class CumulativePitRules {

    public static final BigDecimal MONTHLY_BASIC_DEDUCTION = new BigDecimal("5000");

    private CumulativePitRules() {}

    public record TaxBracket(
            BigDecimal minInclusive,
            BigDecimal maxInclusive,
            BigDecimal rate,
            BigDecimal quickDeduction) {}

    public record YtdInputs(
            BigDecimal priorIncome,
            BigDecimal priorSpecialDeduction,
            BigDecimal priorAdditionalDeduction,
            BigDecimal priorWithheldTax,
            BigDecimal currentIncome,
            BigDecimal currentSpecialDeduction,
            BigDecimal currentAdditionalDeduction,
            int employmentMonths) {}

    public record PitResult(
            BigDecimal cumulativeTaxableIncome,
            BigDecimal cumulativeTax,
            BigDecimal periodTax) {}

    public static int employmentMonths(YearMonth belongMonth, LocalDate entryDate) {
        if (belongMonth == null) {
            return 0;
        }
        YearMonth yearStart = YearMonth.of(belongMonth.getYear(), 1);
        YearMonth start = yearStart;
        if (entryDate != null) {
            YearMonth entryMonth = YearMonth.from(entryDate);
            if (entryMonth.isAfter(belongMonth)) {
                return 0;
            }
            if (!entryMonth.isBefore(yearStart)) {
                start = entryMonth;
            }
            // entry before yearStart -> start remains yearStart
        }
        if (start.isAfter(belongMonth)) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(start, belongMonth) + 1;
    }

    public static PitResult compute(YtdInputs inputs, List<TaxBracket> brackets) {
        BigDecimal cumIncome = nz(inputs.priorIncome()).add(nz(inputs.currentIncome()));
        BigDecimal cumSpecial = nz(inputs.priorSpecialDeduction()).add(nz(inputs.currentSpecialDeduction()));
        BigDecimal cumAdditional = nz(inputs.priorAdditionalDeduction()).add(nz(inputs.currentAdditionalDeduction()));
        int months = Math.max(0, inputs.employmentMonths());
        BigDecimal basic = MONTHLY_BASIC_DEDUCTION.multiply(BigDecimal.valueOf(months));
        BigDecimal taxable = cumIncome.subtract(basic).subtract(cumSpecial).subtract(cumAdditional)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cumulativeTax = applyBracket(taxable, brackets).setScale(2, RoundingMode.HALF_UP);
        BigDecimal period = cumulativeTax.subtract(nz(inputs.priorWithheldTax()))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return new PitResult(taxable, cumulativeTax, period);
    }

    static BigDecimal applyBracket(BigDecimal taxable, List<TaxBracket> brackets) {
        if (taxable.signum() <= 0 || brackets == null || brackets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        for (TaxBracket b : brackets) {
            BigDecimal min = nz(b.minInclusive());
            BigDecimal max = b.maxInclusive() != null ? b.maxInclusive() : new BigDecimal("999999999");
            if (taxable.compareTo(min) >= 0 && taxable.compareTo(max) <= 0) {
                return taxable.multiply(nz(b.rate())).subtract(nz(b.quickDeduction()));
            }
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
