package com.financial.cloud.service.hr;

import com.financial.cloud.service.book.MonthEndCloseRules;
import org.apache.commons.lang3.StringUtils;

/**
 * First-come hard mutex between payroll detail {@code jt_gz} and month-end summary {@code jt_gz}.
 * Labor templates ({@code fp_lwf}) are out of scope.
 */
public final class SalaryAccrualMutexRules {

	public static final String BLOCK_DETAIL_BECAUSE_MONTH_END =
			"本期已通过期末结转生成工资计提凭证，请勿再按明细计提，以免重复入账";

	public static final String BLOCK_MONTH_END_BECAUSE_DETAIL =
			"本期已有按员工明细生成的工资计提凭证，请勿再通过期末结转汇总计提，以免重复入账";

	private SalaryAccrualMutexRules() {
	}

	public static boolean isWageSalaryAccrualTemplate(String templateCode) {
		return templateCode != null && templateCode.startsWith(MonthEndCloseRules.CODE_ACCRUE_SALARY);
	}

	/**
	 * Detail rows that used wage accrual ({@code jt_gz}), not labor invoice.
	 */
	public static boolean countsAsWageDetailAccrual(String employeeType, String accrualVoucherId) {
		return StringUtils.isNotBlank(accrualVoucherId)
				&& !SalaryVoucherTemplateRules.isLaborEmployee(employeeType);
	}
}
