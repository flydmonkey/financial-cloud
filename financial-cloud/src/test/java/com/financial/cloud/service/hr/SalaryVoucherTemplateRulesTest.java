package com.financial.cloud.service.hr;

import org.junit.jupiter.api.Test;
import com.financial.cloud.constants.auth.ConstsUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaryVoucherTemplateRulesTest {

    @Test
    void normalUsesJtGzAndZfGz() {
        assertEquals("jt_gz", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.NORMAL, 2));
        assertEquals("zf_gz", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.NORMAL, 3));
        assertEquals("jt_gz", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.INTERN, 2));
        assertEquals("jt_gz", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.RETIREMENT, 2));
    }

    @Test
    void parttimeUsesLaborTemplates() {
        assertEquals("fp_lwf", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 2));
        assertEquals("zf_lwf", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 3));
        assertTrue(SalaryVoucherTemplateRules.isLaborEmployee(ConstsUser.EMPLOYEE_TYPE.PARTTIME));
        assertFalse(SalaryVoucherTemplateRules.isLaborEmployee(ConstsUser.EMPLOYEE_TYPE.NORMAL));
    }

    @Test
    void alreadyGeneratedMessages() {
        assertEquals("计提凭证已生成", SalaryVoucherTemplateRules.alreadyGeneratedMessage(ConstsUser.EMPLOYEE_TYPE.NORMAL, 2));
        assertEquals("发放凭证已生成", SalaryVoucherTemplateRules.alreadyGeneratedMessage(ConstsUser.EMPLOYEE_TYPE.NORMAL, 3));
        assertEquals("收票凭证已生成", SalaryVoucherTemplateRules.alreadyGeneratedMessage(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 2));
        assertEquals("发放凭证已生成", SalaryVoucherTemplateRules.alreadyGeneratedMessage(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 3));
    }

}
