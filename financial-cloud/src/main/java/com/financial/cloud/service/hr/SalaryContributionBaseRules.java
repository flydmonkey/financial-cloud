package com.financial.cloud.service.hr;

import com.financial.cloud.constants.auth.ConstsUser;
import com.financial.cloud.domain.hr.Employee;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Unified contribution-base resolution for SMB payroll (book default vs per-employee custom).
 */
public final class SalaryContributionBaseRules {

    public static final int SOURCE_BOOK_DEFAULT = 0;
    public static final int SOURCE_EMPLOYEE_CUSTOM = 1;

    private SalaryContributionBaseRules() {
    }

    public record ResolvedBase(BigDecimal amount, int source) {
    }

    /**
     * @return null when the employee type does not use social-insurance base
     */
    public static ResolvedBase resolve(Employee employee, BigDecimal bookDefaultBase) {
        if (employee == null || !ConstsUser.EMPLOYEE_TYPE.NORMAL.equals(employee.getEmployeeType())) {
            return null;
        }
        if (Objects.equals(employee.getPayBaseRule(), 1)) {
            return new ResolvedBase(employee.getPayBaseNumber(), SOURCE_EMPLOYEE_CUSTOM);
        }
        return new ResolvedBase(bookDefaultBase, SOURCE_BOOK_DEFAULT);
    }

    /**
     * @return display name when custom base is incomplete; otherwise null
     */
    public static String incompleteCustomBaseLabel(Employee employee) {
        if (employee == null || !ConstsUser.EMPLOYEE_TYPE.NORMAL.equals(employee.getEmployeeType())) {
            return null;
        }
        if (!Objects.equals(employee.getPayBaseRule(), 1)) {
            return null;
        }
        BigDecimal base = employee.getPayBaseNumber();
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            String name = employee.getDisplayName();
            if (name == null || name.isBlank()) {
                name = employee.getEmployeeNumber() != null ? employee.getEmployeeNumber() : employee.getId();
            }
            return name;
        }
        return null;
    }
}
