package com.financial.cloud.service.book;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Month-end close hard-gate rules for required carry-forward templates.
 * <p>
 * Subject roots follow the book's {@code standardId}:
 * <ul>
 *   <li>{@code 1} — 小企业会计准则</li>
 *   <li>{@code 2} — 企业会计制度</li>
 * </ul>
 * Template codes remain {@code qm_jz_sr} / {@code qm_jz_cbfy} / {@code qm_jz_bnlr}.
 */
public final class MonthEndCloseRules {

	public static final String CODE_CARRY_INCOME = "qm_jz_sr";
	public static final String CODE_CARRY_COST = "qm_jz_cbfy";
	public static final String CODE_CARRY_YEAR_PROFIT = "qm_jz_bnlr";

	public static final String CODE_ACCRUE_SALARY = "jt_gz";
	public static final String CODE_ACCRUE_INCOME_TAX = "jt_sds";
	public static final String CODE_ACCRUE_SURTAX = "jt_fjs";
	/** 工资发放（借：应付职工薪酬 / 贷：银行存款） */
	public static final String CODE_PAY_SALARY = "zf_gz";

	/** @deprecated 已退役：折旧走固定资产模块，不再使用期末模板 {@code jt_zj}. */
	@Deprecated
	public static final String CODE_ACCRUE_DEPRECIATION = "jt_zj";

	private static final Set<String> ACCRUAL_TEMPLATE_CODES = Set.of(
			CODE_ACCRUE_SALARY, CODE_ACCRUE_INCOME_TAX, CODE_ACCRUE_SURTAX);

	private static final Set<String> SALARY_PAYMENT_TEMPLATE_CODES = Set.of(CODE_PAY_SALARY);

	/** Codes retired from book template catalogs (overlap with other modules / carries). */
	private static final Set<String> RETIRED_TEMPLATE_CODES = Set.of("qm_jz_xscb", "jt_zj");

	/** 小企业会计准则 */
	public static final String STANDARD_SMALL_BUSINESS = "1";
	/** 企业会计制度 */
	public static final String STANDARD_ENTERPRISE_SYSTEM = "2";

	/**
	 * @deprecated Prefer {@link #incomeCarryRootsForStandard(String)}. Kept for callers that only know CAS-style codes.
	 */
	@Deprecated
	public static final List<String> INCOME_CARRY_SUBJECT_ROOTS = List.of("6001", "6301", "6051");

	/**
	 * @deprecated Prefer {@link #costCarryRootsForStandard(String)}.
	 */
	@Deprecated
	public static final List<String> COST_CARRY_SUBJECT_ROOTS = List.of(
			"6401", "6405", "6601", "6602", "6603", "6711");

	private static final Set<String> ALWAYS_REQUIRED = Set.of(CODE_CARRY_INCOME, CODE_CARRY_COST);

	private MonthEndCloseRules() {
	}

	public static boolean isAlwaysRequiredCarryCode(String code) {
		return code != null && ALWAYS_REQUIRED.contains(code);
	}

	public static boolean isDecemberYearProfitCode(String code) {
		return CODE_CARRY_YEAR_PROFIT.equals(code);
	}

	public static boolean isEnterpriseAccountingSystem(String standardId) {
		return STANDARD_ENTERPRISE_SYSTEM.equals(standardId);
	}

	/**
	 * Income subjects closed into 本年利润 for the book's accounting standard.
	 */
	public static List<String> incomeCarryRootsForStandard(String standardId) {
		if (isEnterpriseAccountingSystem(standardId)) {
			// 企业会计制度：主营/其他业务收入、投资收益、补贴收入、营业外收入
			return List.of("5101", "5102", "5201", "5203", "5301");
		}
		// 小企业会计准则（默认）
		return List.of("5001", "5051", "5111", "5301");
	}

	/**
	 * Cost/expense subjects closed into 本年利润（不含税金及附加）.
	 * 主营业务成本、其他业务成本、销售/管理/财务费用、营业外支出.
	 */
	public static List<String> costCarryRootsForStandard(String standardId) {
		if (isEnterpriseAccountingSystem(standardId)) {
			return List.of("5401", "5405", "5501", "5502", "5503", "5601");
		}
		return List.of("5401", "5402", "5601", "5602", "5603", "5711");
	}

	/** 本年利润 subject root for the standard. */
	public static String yearProfitSubjectForStandard(String standardId) {
		return isEnterpriseAccountingSystem(standardId) ? "3131" : "3103";
	}

	/** 未分配利润（年末结转本年利润对方科目）. */
	public static String undistributedProfitSubjectForStandard(String standardId) {
		return isEnterpriseAccountingSystem(standardId) ? "3141.15" : "3104.02";
	}

	/** 所得税费用（可选结转）. */
	public static String incomeTaxExpenseSubjectForStandard(String standardId) {
		return isEnterpriseAccountingSystem(standardId) ? "5701" : "5801";
	}

	public static boolean isRetiredTemplateCode(String code) {
		return code != null && RETIRED_TEMPLATE_CODES.contains(code);
	}

	public static boolean isAccrualTemplateCode(String code) {
		return code != null && ACCRUAL_TEMPLATE_CODES.contains(code);
	}

	public static Set<String> accrualTemplateCodes() {
		return ACCRUAL_TEMPLATE_CODES;
	}

	public static Set<String> salaryPaymentTemplateCodes() {
		return SALARY_PAYMENT_TEMPLATE_CODES;
	}

	public static boolean isSalaryPaymentTemplateCode(String code) {
		return code != null && SALARY_PAYMENT_TEMPLATE_CODES.contains(code);
	}

	/**
	 * One default voucher-template line for P&amp;L carry (direction 1=借 2=贷).
	 */
	public record CarryTemplateItemSpec(String subjectCode, int direction, String summary) {
	}

	/** Default template lines for {@code qm_jz_sr} under the given standard. */
	public static List<CarryTemplateItemSpec> defaultIncomeCarryTemplateItems(String standardId) {
		List<CarryTemplateItemSpec> items = new ArrayList<>();
		for (String code : incomeCarryRootsForStandard(standardId)) {
			items.add(new CarryTemplateItemSpec(code, 1, "结转收入"));
		}
		items.add(new CarryTemplateItemSpec(yearProfitSubjectForStandard(standardId), 2, "结转收入"));
		return items;
	}

	/** Default template lines for {@code qm_jz_cbfy} under the given standard. */
	public static List<CarryTemplateItemSpec> defaultCostCarryTemplateItems(String standardId) {
		List<CarryTemplateItemSpec> items = new ArrayList<>();
		for (String code : costCarryRootsForStandard(standardId)) {
			items.add(new CarryTemplateItemSpec(code, 2, "结转成本费用"));
		}
		items.add(new CarryTemplateItemSpec(yearProfitSubjectForStandard(standardId), 1, "结转成本费用"));
		return items;
	}

	/** Default two-line accrual templates for the given standard. */
	public static List<CarryTemplateItemSpec> defaultAccrualTemplateItems(String templateCode, String standardId) {
		boolean enterprise = isEnterpriseAccountingSystem(standardId);
		return switch (templateCode == null ? "" : templateCode) {
			case CODE_ACCRUE_SALARY -> List.of(
					// 小企业：末级科目；企业制度：父级编码，生成凭证时再解析末级
					new CarryTemplateItemSpec(enterprise ? "5502" : "5602.07", 1, "计提工资"),
					new CarryTemplateItemSpec(enterprise ? "2151" : "2211.01", 2, "计提工资"));
			case CODE_ACCRUE_INCOME_TAX -> List.of(
					new CarryTemplateItemSpec(enterprise ? "5701" : "5801", 1, "计提所得税"),
					new CarryTemplateItemSpec(enterprise ? "2171.06" : "2221.05", 2, "计提所得税"));
			case CODE_ACCRUE_SURTAX -> List.of(
					new CarryTemplateItemSpec(enterprise ? "5402" : "5403", 1, "计提附加税"),
					new CarryTemplateItemSpec(enterprise ? "2171" : "2221", 2, "计提附加税"));
			default -> List.of();
		};
	}

	/** Default wage-payment lines: 借应付职工薪酬 / 贷银行存款. */
	public static List<CarryTemplateItemSpec> defaultPaySalaryTemplateItems(String standardId) {
		boolean enterprise = isEnterpriseAccountingSystem(standardId);
		return List.of(
				new CarryTemplateItemSpec(enterprise ? "2151" : "2211.01", 1, "发放工资"),
				new CarryTemplateItemSpec("1002", 2, "发放工资"));
	}

	public static List<CarryTemplateItemSpec> defaultCarryTemplateItems(String templateCode, String standardId) {
		if (CODE_CARRY_INCOME.equals(templateCode)) {
			return defaultIncomeCarryTemplateItems(standardId);
		}
		if (CODE_CARRY_COST.equals(templateCode)) {
			return defaultCostCarryTemplateItems(standardId);
		}
		if (isAccrualTemplateCode(templateCode)) {
			return defaultAccrualTemplateItems(templateCode, standardId);
		}
		if (isSalaryPaymentTemplateCode(templateCode)) {
			return defaultPaySalaryTemplateItems(standardId);
		}
		return List.of();
	}

	/** Codes that get auto-seeded empty template lines when creating a book. */
	public static boolean isAutoSeedTemplateCode(String code) {
		return isAlwaysRequiredCarryCode(code) || isAccrualTemplateCode(code)
				|| isSalaryPaymentTemplateCode(code);
	}

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
