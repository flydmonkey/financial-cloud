package com.financial.cloud.service.book;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Month-end close hard-gate rules for required carry-forward templates.
 * <p>
 * Discovery (task 1.1): {@code voucher_template} has no {@code required_for_close} column.
 * Category {@code 1} means 期末处理 but also includes optional accruals (计提折旧/所得税等).
 * Chosen rule — code allowlist aligned with E2E / SettlementCarryService:
 * <ul>
 *   <li>Every month: {@code qm_jz_sr} (结转收入), {@code qm_jz_cbfy} (结转成本费用)</li>
 *   <li>December only: {@code qm_jz_bnlr} (结转本年利润) — still only month-end, not a year-end entry</li>
 *   <li>Optional (not hard): {@code qm_jz_sds}, payroll {@code jt_*}/{@code zf_*}, {@code jt_zj}, etc.</li>
 * </ul>
 */
public final class MonthEndCloseRules {

	public static final String CODE_CARRY_INCOME = "qm_jz_sr";
	public static final String CODE_CARRY_COST = "qm_jz_cbfy";
	public static final String CODE_CARRY_YEAR_PROFIT = "qm_jz_bnlr";

	private static final Set<String> ALWAYS_REQUIRED = Set.of(CODE_CARRY_INCOME, CODE_CARRY_COST);

	private MonthEndCloseRules() {
	}

	public static boolean isAlwaysRequiredCarryCode(String code) {
		return code != null && ALWAYS_REQUIRED.contains(code);
	}

	public static boolean isDecemberYearProfitCode(String code) {
		return CODE_CARRY_YEAR_PROFIT.equals(code);
	}

	/**
	 * Template codes that must have a settlement_carryforward voucher for the given term.
	 */
	public static List<String> requiredCarryCodesForTerm(String yearPeriod) {
		List<String> codes = new ArrayList<>(ALWAYS_REQUIRED);
		if (isDecember(yearPeriod)) {
			codes.add(CODE_CARRY_YEAR_PROFIT);
		}
		return codes;
	}

	public static boolean isDecember(String yearPeriod) {
		if (yearPeriod == null || yearPeriod.length() < 7) {
			return false;
		}
		return YearMonth.parse(yearPeriod).getMonthValue() == 12;
	}
}
