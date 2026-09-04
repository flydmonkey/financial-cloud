package com.financial.cloud.service.hr;

import org.junit.jupiter.api.Test;

import com.financial.cloud.constants.auth.ConstsUser;
import com.financial.cloud.domain.hr.Employee;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SalaryContributionBaseRulesTest {

    @Test
    void customBaseUsedWhenRuleEnabled() {
        Employee e = normal("张三");
        e.setPayBaseRule(1);
        e.setPayBaseNumber(new BigDecimal("8000.00"));
        SalaryContributionBaseRules.ResolvedBase resolved =
                SalaryContributionBaseRules.resolve(e, new BigDecimal("5000"));
        assertNotNull(resolved);
        assertEquals(0, new BigDecimal("8000.00").compareTo(resolved.amount()));
        assertEquals(SalaryContributionBaseRules.SOURCE_EMPLOYEE_CUSTOM, resolved.source());
    }

    @Test
    void bookDefaultUsedWhenRuleSystem() {
        Employee e = normal("李四");
        e.setPayBaseRule(0);
        SalaryContributionBaseRules.ResolvedBase resolved =
                SalaryContributionBaseRules.resolve(e, new BigDecimal("6500.50"));
        assertNotNull(resolved);
        assertEquals(0, new BigDecimal("6500.50").compareTo(resolved.amount()));
        assertEquals(SalaryContributionBaseRules.SOURCE_BOOK_DEFAULT, resolved.source());
    }

    @Test
    void incompleteCustomBaseDetected() {
        Employee e = normal("王五");
        e.setPayBaseRule(1);
        e.setPayBaseNumber(null);
        assertEquals("王五", SalaryContributionBaseRules.incompleteCustomBaseLabel(e));

        e.setPayBaseNumber(BigDecimal.ZERO);
        assertEquals("王五", SalaryContributionBaseRules.incompleteCustomBaseLabel(e));

        e.setPayBaseNumber(new BigDecimal("1"));
        assertNull(SalaryContributionBaseRules.incompleteCustomBaseLabel(e));
    }

    @Test
    void nonNormalSkipsBase() {
        Employee e = new Employee();
        e.setEmployeeType(ConstsUser.EMPLOYEE_TYPE.PARTTIME);
        e.setPayBaseRule(1);
        assertNull(SalaryContributionBaseRules.resolve(e, new BigDecimal("5000")));
        assertNull(SalaryContributionBaseRules.incompleteCustomBaseLabel(e));
    }

    private static Employee normal(String name) {
        Employee e = new Employee();
        e.setId("e1");
        e.setDisplayName(name);
        e.setEmployeeType(ConstsUser.EMPLOYEE_TYPE.NORMAL);
        return e;
    }
}
