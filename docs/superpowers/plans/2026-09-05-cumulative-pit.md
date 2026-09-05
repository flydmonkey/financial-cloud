# Cumulative PIT (SMB) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace monthly wage PIT bracket-on-month with cumulative withholding for NORMAL/INTERN/RETIREMENT while leaving labor tax unchanged.

**Architecture:** Pure `CumulativePitRules` computes period tax from YTD inputs + tax brackets. `EmployeeSalaryTempService` loads prior confirmed `employee_salary` rows for the year, builds current-month inputs from existing pay/SI/deduction calc, writes `personalTax` (period) and `taxableWages` (cumulative taxable income).

**Tech Stack:** Java 17, JUnit 5, MyBatis-Plus, existing `ConfigPersonalTax` brackets (type=0).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-09-05-cumulative-pit-design.md`
- No new DB tables
- Labor (`PARTTIME`) path unchanged (`calculatePersonalTax(..., 1)`)
- Money: `HALF_UP` scale 2
- Basic deduction: `5000 × employmentMonths`
- `taxableWages` field meaning becomes **累计应纳税所得额** for wage employees
- Frequent commits; TDD for rules first

## File map

| File | Role |
|------|------|
| `financial-cloud/src/main/java/com/financial/cloud/service/hr/CumulativePitRules.java` | Pure cumulative formula + bracket apply |
| `financial-cloud/src/test/java/com/financial/cloud/service/hr/CumulativePitRulesTest.java` | Unit tests |
| `financial-cloud/src/main/java/com/financial/cloud/service/hr/EmployeeSalaryTempService.java` | Wire YTD load + wage path |
| `docs/product/09-payroll.md` | Product wording |
| `financial-cloud-ui/src/views/hr/calc-salary.vue` | One-line hint that PIT is cumulative |

---

### Task 1: CumulativePitRules (TDD)

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/service/hr/CumulativePitRules.java`
- Test: `financial-cloud/src/test/java/com/financial/cloud/service/hr/CumulativePitRulesTest.java`

**Interfaces:**
- Produces:
  - `record TaxBracket(BigDecimal minInclusive, BigDecimal maxInclusive, BigDecimal rate, BigDecimal quickDeduction)`
  - `record YtdInputs(BigDecimal priorIncome, BigDecimal priorSpecialDeduction, BigDecimal priorAdditionalDeduction, BigDecimal priorWithheldTax, BigDecimal currentIncome, BigDecimal currentSpecialDeduction, BigDecimal currentAdditionalDeduction, int employmentMonths)`
  - `record PitResult(BigDecimal cumulativeTaxableIncome, BigDecimal cumulativeTax, BigDecimal periodTax)`
  - `static int employmentMonths(YearMonth belongMonth, java.time.LocalDate entryDate)`
  - `static PitResult compute(YtdInputs inputs, List<TaxBracket> brackets)`
  - `static TaxBracket fromConfig(ConfigPersonalTax row)` helper optional inside rules or service

- [ ] **Step 1: Write failing tests**

Create `CumulativePitRulesTest.java`:

```java
package com.financial.cloud.service.hr;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        // cum tax 120, prior withheld 500 → period 0
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
```

- [ ] **Step 2: Run tests — expect fail**

```bash
cd financial-cloud
mvn "-Dtest=CumulativePitRulesTest" test
```

Expected: compilation failure / class not found.

- [ ] **Step 3: Implement `CumulativePitRules`**

```java
package com.financial.cloud.service.hr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
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
        YearMonth start = YearMonth.of(belongMonth.getYear(), 1);
        if (entryDate != null) {
            YearMonth entryMonth = YearMonth.from(entryDate);
            if (entryMonth.getYear() == belongMonth.getYear() && entryMonth.isAfter(start)) {
                start = entryMonth;
            } else if (entryMonth.getYear() > belongMonth.getYear()) {
                return 0;
            } else if (entryMonth.getYear() == belongMonth.getYear() && entryMonth.isAfter(belongMonth)) {
                return 0;
            }
        }
        if (start.isAfter(belongMonth)) {
            return 0;
        }
        return (int) (start.until(belongMonth, java.time.temporal.ChronoUnit.MONTHS) + 1);
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
```

Fix `employmentMonths` for entry in prior years: if `entryDate` year &lt; belong year, start stays Jan. If entry year == belong year and entry after Jan, start = entry month. If entry after belong month, return 0.

Refine `employmentMonths` implementation carefully:

```java
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
        // entry before yearStart → start remains yearStart
    }
    if (start.isAfter(belongMonth)) {
        return 0;
    }
    return (int) ChronoUnit.MONTHS.between(start, belongMonth) + 1;
}
```

- [ ] **Step 4: Run tests — expect pass**

```bash
mvn "-Dtest=CumulativePitRulesTest" test
```

Expected: all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add financial-cloud/src/main/java/com/financial/cloud/service/hr/CumulativePitRules.java \
  financial-cloud/src/test/java/com/financial/cloud/service/hr/CumulativePitRulesTest.java
git commit -m "feat: add CumulativePitRules for wage withholding"
```

---

### Task 2: Wire EmployeeSalaryTempService

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/hr/EmployeeSalaryTempService.java`
- Depends on: `EmployeeSalaryMapper` (already may need inject — check constructors; currently has mappers listed at top of class)

**Interfaces:**
- Consumes: `CumulativePitRules.compute`, `employmentMonths`
- Produces: wage path sets `personalTax` = period tax, `taxableWages` = cumulative taxable income

- [ ] **Step 1: Ensure `EmployeeSalaryMapper` is available**

If missing, add:

```java
private final EmployeeSalaryMapper employeeSalaryMapper;
```

(`RequiredArgsConstructor` will wire it.)

- [ ] **Step 2: Add helpers on the service**

```java
private List<CumulativePitRules.TaxBracket> loadWageBrackets() {
    return configPersonalTaxMapper.selectList(Wrappers.<ConfigPersonalTax>lambdaQuery()
            .eq(ConfigPersonalTax::getType, 0)
            .orderByAsc(ConfigPersonalTax::getLevel))
            .stream()
            .map(t -> new CumulativePitRules.TaxBracket(
                    t.getMinNum() != null ? BigDecimal.valueOf(t.getMinNum()) : BigDecimal.ZERO,
                    t.getMaxNum() != null ? BigDecimal.valueOf(t.getMaxNum()) : new BigDecimal("999999999"),
                    BigDecimal.valueOf(t.getTaxRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP),
                    t.getCalculationDeduction() != null
                            ? BigDecimal.valueOf(t.getCalculationDeduction())
                            : BigDecimal.ZERO))
            .toList();
}

private CumulativePitRules.YtdInputs buildYtdInputs(
        String bookId, String employeeId, YearMonth belongMonth,
        BigDecimal currentIncome, BigDecimal currentSpecial, BigDecimal currentAdditional,
        Date entryDate) {
    YearMonth yearStart = YearMonth.of(belongMonth.getYear(), 1);
    List<EmployeeSalary> priors = employeeSalaryMapper.selectList(Wrappers.<EmployeeSalary>lambdaQuery()
            .eq(EmployeeSalary::getBookId, bookId)
            .eq(EmployeeSalary::getEmployeeId, employeeId)
            .eq(EmployeeSalary::getDeleted, "n")
            .ge(EmployeeSalary::getBelongDate, yearStart)
            .lt(EmployeeSalary::getBelongDate, belongMonth));
    BigDecimal priorIncome = BigDecimal.ZERO;
    BigDecimal priorSpecial = BigDecimal.ZERO;
    BigDecimal priorAdditional = BigDecimal.ZERO;
    BigDecimal priorTax = BigDecimal.ZERO;
    if (priors != null) {
        for (EmployeeSalary row : priors) {
            priorIncome = priorIncome.add(NumberUtil.nullToZero(row.getPayAmount()));
            priorSpecial = priorSpecial.add(NumberUtil.nullToZero(row.getTotalSocialInsurance()))
                    .add(NumberUtil.nullToZero(row.getProvidentFund()));
            priorAdditional = priorAdditional.add(NumberUtil.nullToZero(row.getTaxDeduction()));
            priorTax = priorTax.add(NumberUtil.nullToZero(row.getPersonalTax()));
        }
    }
    LocalDate entry = entryDate == null ? null
            : entryDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    // Prefer: if entryDate is java.util.Date, convert safely; Employee.entryDate type is Date
    int months = CumulativePitRules.employmentMonths(belongMonth, entry);
    return new CumulativePitRules.YtdInputs(
            priorIncome, priorSpecial, priorAdditional, priorTax,
            NumberUtil.nullToZero(currentIncome),
            NumberUtil.nullToZero(currentSpecial),
            NumberUtil.nullToZero(currentAdditional),
            months);
}

private void applyCumulativePit(
        EmployeeSalaryTemp row, Employee employee, YearMonth belongMonth,
        List<CumulativePitRules.TaxBracket> brackets) {
    BigDecimal currentSpecial = NumberUtil.nullToZero(row.getTotalSocialInsurance())
            .add(NumberUtil.nullToZero(row.getProvidentFund()));
    CumulativePitRules.YtdInputs inputs = buildYtdInputs(
            row.getBookId(), row.getEmployeeId(), belongMonth,
            row.getPayAmount(), currentSpecial, row.getTaxDeduction(),
            employee.getEntryDate());
    CumulativePitRules.PitResult pit = CumulativePitRules.compute(inputs, brackets);
    row.setTaxableWages(pit.cumulativeTaxableIncome());
    row.setPersonalTax(pit.periodTax());
}
```

Convert `Date` → `LocalDate` using project’s existing date utils if any; otherwise:

```java
private static LocalDate toLocalDate(Date date) {
    if (date == null) return null;
    if (date instanceof java.sql.Date sql) return sql.toLocalDate();
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
}
```

- [ ] **Step 3: Change `calculateSalary` wage path**

Replace putting old `taxableWages` into `needPersonalTaxMap` and applying `calculatePersonalTax(..., 0)` for wages.

After `calculateOtherValue` for wage employees, **do not** put into `needPersonalTaxMap`. After the loop:

```java
List<CumulativePitRules.TaxBracket> wageBrackets = loadWageBrackets();
Map<String, Employee> employeeById = employees.stream()
        .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));

for (EmployeeSalaryTemp salaryDetail : employeeSalaryTemps) {
    // existing salaryAmount check...
    if (isWageEmployee(salaryDetail.getEmployeeType()) && salaryAmount > 0) {
        Employee emp = employeeById.get(salaryDetail.getEmployeeId());
        applyCumulativePit(salaryDetail, emp, dto.getLastMonth(), wageBrackets);
        BigDecimal personalTax = salaryDetail.getPersonalTax();
        salaryDetail.setTotalAmount(salaryDetail.getTotalAmount().subtract(personalTax).max(BigDecimal.ZERO));
        salaryDetail.setBusinessExpenditureCosts(... same as today ...);
    } else {
        // labor: keep calculatePersonalTax map path
    }
}
```

Keep `needLaborTaxMap` + `calculatePersonalTax(..., 1)` for parttime only.

Also stop setting pre-tax `taxableWages` as the old “totalAmount − taxDeduction” **before** cumulative for wage staff — either leave temporary then overwrite in `applyCumulativePit`, or skip old taxable assignment for wages in `calculateOtherValue` by overwriting later (overwrite is fine).

- [ ] **Step 4: Change single-row recalculate path** (`update` / preview recalculate around lines 125–141)

Same: for wage types call `applyCumulativePit` with belong date from dto/temp; for labor keep old.

- [ ] **Step 5: Compile / focused test**

```bash
mvn "-Dtest=CumulativePitRulesTest" test
mvn -DskipTests compile
```

Expected: SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add financial-cloud/src/main/java/com/financial/cloud/service/hr/EmployeeSalaryTempService.java
git commit -m "feat: wire cumulative PIT into salary calculation"
```

---

### Task 3: Docs + minimal UI hint

**Files:**
- Modify: `docs/product/09-payroll.md`
- Modify: `financial-cloud-ui/src/views/hr/calc-salary.vue` (one alert/hint near 个税 column or page tip)

- [ ] **Step 1: Update product doc**

In `09-payroll.md`:

- Replace non-goal line about 累计预扣 with **已实现（SMB）** summary: formula bullets + labor excluded + no prior-employer import.
- Note `taxableWages` = 累计应纳税所得额 for wage employees.
- Note: changing confirmed history months requires recalculating later months.

- [ ] **Step 2: UI hint**

On `calc-salary.vue`, add a short `el-alert` or existing tip area text:

`普通员工个税已按综合所得累计预扣计算（减除费用 5000×任职月数，并扣减本年已预扣）。`

- [ ] **Step 3: Commit**

```bash
git add docs/product/09-payroll.md financial-cloud-ui/src/views/hr/calc-salary.vue
git commit -m "docs: document cumulative PIT and add calc-salary hint"
```

---

### Task 4: Verification

- [x] **Step 1: Run unit tests**

```bash
cd financial-cloud
mvn "-Dtest=CumulativePitRulesTest,SalaryContributionBaseRulesTest,SalaryAccrualMutexRulesTest" test
```

Expected: PASS.

- [x] **Step 2: Manual smoke (optional if env up)** — skipped (env unavailable)

1. Login → calc-salary generate preview for a NORMAL employee with no prior year salary → expect tax ≈ (income − 5000 − SI − additional) × 3% when in first bracket.
2. Confirm/push month 1, then preview month 2 → period tax ≈ second-month cumulative tax − month1 personalTax.
3. Part-time employee tax unchanged vs prior behavior.

- [x] **Step 3: Mark plan tasks complete in this file** (checkboxes).

---

## Spec coverage checklist

| Spec item | Task |
|-----------|------|
| Cumulative formula + 5000×months | Task 1 |
| Prior from employee_salary | Task 2 |
| Wage types only; labor unchanged | Task 2 |
| taxableWages = cumulative taxable | Task 2 |
| personalTax = period tax | Task 2 |
| Unit tests listed in spec | Task 1 |
| Product doc | Task 3 |
| Minimal UI | Task 3 |
| No new tables / no prior-employer import | Global |

## Self-review

- No TBD placeholders in steps.
- Bracket table in tests matches national cumulative annual brackets; production still uses DB `config_personal_tax` — if DB still has **monthly** bracket thresholds, document risk (already in spec); implementer should spot-check seed data and note in commit if thresholds look monthly (e.g. max 3000) vs annual (36000).
