package com.financial.cloud.service.hr;

import org.junit.jupiter.api.Test;

import com.financial.cloud.constants.auth.ConstsUser;
import com.financial.cloud.domain.hr.Employee;
import com.financial.cloud.enums.error.HrErrorCode;
import com.financial.cloud.exception.BusinessException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeServicePayBaseTest {

    @Test
    void rejectsSaveWhenCustomBaseMissing() {
        Employee e = normal("工资员");
        e.setPayBaseRule(1);
        e.setPayBaseNumber(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> EmployeeService.rejectIncompleteCustomPayBase(e));
        assertEquals(HrErrorCode.CUSTOM_PAY_BASE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void rejectsSaveWhenCustomBaseNonPositive() {
        Employee e = normal("工资员");
        e.setPayBaseRule(1);
        e.setPayBaseNumber(BigDecimal.ZERO);

        assertThrows(BusinessException.class, () -> EmployeeService.rejectIncompleteCustomPayBase(e));
    }

    @Test
    void allowsCustomBaseWhenPositive() {
        Employee e = normal("工资员");
        e.setPayBaseRule(1);
        e.setPayBaseNumber(new BigDecimal("5000"));
        assertDoesNotThrow(() -> EmployeeService.rejectIncompleteCustomPayBase(e));
    }

    @Test
    void allowsBookDefaultRuleWithoutCustomAmount() {
        Employee e = normal("工资员");
        e.setPayBaseRule(0);
        e.setPayBaseNumber(null);
        assertDoesNotThrow(() -> EmployeeService.rejectIncompleteCustomPayBase(e));
    }

    private static Employee normal(String name) {
        Employee e = new Employee();
        e.setDisplayName(name);
        e.setEmployeeType(ConstsUser.EMPLOYEE_TYPE.NORMAL);
        return e;
    }
}
