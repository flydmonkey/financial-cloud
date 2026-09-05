package com.financial.cloud.service.hr;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CumulativePitRulesTest {

    private static final List<CumulativePitRules.TaxBracket> BRACKETS = List.of(
            new CumulativePitRules.TaxBracket(bd("0"), bd("36000"), bd("0.03"), bd("0")),
            new CumulativePitRules.TaxBracket(bd("36000"), bd("144000"), bd("0.10"), bd("2520")),
            new CumulativePitRules.TaxBracket(bd("144000"), bd("300000"), bd("0.20"), bd("16920")),
            new CumulativePitRules.TaxBracket(bd("300000"), bd("420000"), bd("0.25"), bd("31920")),
            new CumulativePitRules.TaxBracket(bd("420000"), bd("660000"), bd("0.30"), bd("52920")),
            new CumulativePitRules.TaxBracket(bd("660000"), bd("960000"), bd("0.35"), bd("85920")),
            new CumulativePitRules.TaxBracket(bd("960000"), new BigDecimal("999999999"), bd("0.45"), bd("181920"))
    );

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void firstMonthLowBracket() {
        // income 10000, SI+HF 2000, additional 1000, months=1
        // taxable = 10000 - 5000 - 2000 - 1000 = 2000; tax = 2000*0.03 = 60
        var inputs = new CumulativePitRules.YtdInputs(
                bd("0"), bd("0"), bd("0"), bd("0"),
                bd("10000"), bd("2000"), bd("1000"), 1);
        var result = CumulativePitRules.compute(inputs, BRACKETS);
        assertEquals(0, bd("2000.00").compareTo(result.cumulativeTaxableIncome()));
        assertEquals(0, bd("60.00").compareTo(result.periodTax()));
    }

    @Test
    void secondMonthSubtractsPriorWithheld() {
        // prior: income 10000, special 2000, add 1000, withheld 60, months will be 2
        // current same 10000/2000/1000
        // cum income 20000 - 10000 - 4000 - 2000 = 4000; tax 120; period = 120-60 = 60
        var inputs = new CumulativePitRules.YtdInputs(
                bd("10000"), bd("2000"), bd("1000"), bd("60"),
                bd("10000"), bd("2000"), bd("1000"), 2);
        var result = CumulativePitRules.compute(inputs, BRACKETS);
        assertEquals(0, bd("4000.00").compareTo(result.cumulativeTaxableIncome()));
        assertEquals(0, bd("60.00").compareTo(result.periodTax()));
    }

    @Test
    void crossesIntoSecondBracket() {
        // cum taxable just above 36000
        var inputs = new CumulativePitRules.YtdInputs(
                bd("0"), bd("0"), bd("0"), bd("0"),
                bd("50000"), bd("0"), bd("0"), 1);
        // taxable = 50000 - 5000 = 45000; tax = 45000*0.1 - 2520 = 1980
        var result = CumulativePitRules.compute(inputs, BRACKETS);
        assertEquals(0, bd("45000.00").compareTo(result.cumulativeTaxableIncome()));
        assertEquals(0, bd("1980.00").compareTo(result.periodTax()));
    }

    @Test
    void periodTaxNeverNegative() {
        var inputs = new CumulativePitRules.YtdInputs(
                bd("10000"), bd("2000"), bd("1000"), bd("500"),
                bd("10000"), bd("2000"), bd("1000"), 2);
        // cum tax 120, prior withheld 500 -> period 0
        var result = CumulativePitRules.compute(inputs, BRACKETS);
        assertEquals(0, bd("0.00").compareTo(result.periodTax()));
    }

    @Test
    void employmentMonthsFromJanuaryWhenNoEntryDate() {
        assertEquals(3, CumulativePitRules.employmentMonths(YearMonth.of(2026, 3), null));
    }

    @Test
    void employmentMonthsFromEntryMonth() {
        assertEquals(2, CumulativePitRules.employmentMonths(
                YearMonth.of(2026, 3), LocalDate.of(2026, 2, 15)));
    }

    @Test
    void employmentMonthsZeroWhenEntryAfterBelong() {
        assertEquals(0, CumulativePitRules.employmentMonths(
                YearMonth.of(2026, 3), LocalDate.of(2026, 4, 1)));
    }

    @Test
    void zeroTaxableWhenDeductionsExceedIncome() {
        var inputs = new CumulativePitRules.YtdInputs(
                bd("0"), bd("0"), bd("0"), bd("0"),
                bd("4000"), bd("0"), bd("0"), 1);
        var result = CumulativePitRules.compute(inputs, BRACKETS);
        assertEquals(0, bd("0.00").compareTo(result.cumulativeTaxableIncome()));
        assertEquals(0, bd("0.00").compareTo(result.periodTax()));
    }
}
