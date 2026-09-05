package com.financial.cloud.service.hr;

import com.financial.cloud.constants.auth.ConstsUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaryAccrualMutexRulesTest {

	@Test
	void recognizesWageAccrualTemplate() {
		assertTrue(SalaryAccrualMutexRules.isWageSalaryAccrualTemplate("jt_gz"));
		assertTrue(SalaryAccrualMutexRules.isWageSalaryAccrualTemplate("jt_gz_extra"));
		assertFalse(SalaryAccrualMutexRules.isWageSalaryAccrualTemplate("fp_lwf"));
		assertFalse(SalaryAccrualMutexRules.isWageSalaryAccrualTemplate("zf_gz"));
		assertFalse(SalaryAccrualMutexRules.isWageSalaryAccrualTemplate(null));
	}

	@Test
	void detailAccrualIgnoresLaborAndBlank() {
		assertTrue(SalaryAccrualMutexRules.countsAsWageDetailAccrual(
				ConstsUser.EMPLOYEE_TYPE.NORMAL, "v1"));
		assertTrue(SalaryAccrualMutexRules.countsAsWageDetailAccrual(
				ConstsUser.EMPLOYEE_TYPE.INTERN, "v1"));
		assertFalse(SalaryAccrualMutexRules.countsAsWageDetailAccrual(
				ConstsUser.EMPLOYEE_TYPE.PARTTIME, "v1"));
		assertFalse(SalaryAccrualMutexRules.countsAsWageDetailAccrual(
				ConstsUser.EMPLOYEE_TYPE.NORMAL, null));
		assertFalse(SalaryAccrualMutexRules.countsAsWageDetailAccrual(
				ConstsUser.EMPLOYEE_TYPE.NORMAL, "  "));
	}

	@Test
	void messagesAreDistinct() {
		assertEquals(
				"本期已通过期末结转生成工资计提凭证，请勿再按明细计提，以免重复入账",
				SalaryAccrualMutexRules.BLOCK_DETAIL_BECAUSE_MONTH_END);
		assertEquals(
				"本期已有按员工明细生成的工资计提凭证，请勿再通过期末结转汇总计提，以免重复入账",
				SalaryAccrualMutexRules.BLOCK_MONTH_END_BECAUSE_DETAIL);
	}
}
