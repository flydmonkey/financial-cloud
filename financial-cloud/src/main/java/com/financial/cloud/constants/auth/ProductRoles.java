package com.financial.cloud.constants.auth;

import java.util.Collection;
import java.util.Set;

import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.authn.core.Authority;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;

/**
 * Product RBAC helpers for the four standard roles (active book).
 * Legacy ROLE_SUPERVISOR / "1000" are not product bypasses.
 */
public final class ProductRoles {

    public static final String ADMINISTRATORS = "ROLE_ADMINISTRATORS";
    public static final String BOOKKEEPER = "ROLE_BOOKKEEPER";
    public static final String REVIEWER = "ROLE_REVIEWER";
    public static final String VIEWER = "ROLE_VIEWER";

    public static final Set<String> PRODUCT_ROLE_IDS = Set.of(
            ADMINISTRATORS, BOOKKEEPER, REVIEWER, VIEWER);

    /** Write vouchers / journal / assets / payroll / ARAP mutations. */
    private static final Set<String> WRITE_BUSINESS = Set.of(
            ADMINISTRATORS, BOOKKEEPER, REVIEWER);

    private static final Set<String> APPROVE_OR_CLOSE = Set.of(
            ADMINISTRATORS, REVIEWER);

    private static final Set<String> ADMINISTRATOR_ONLY = Set.of(ADMINISTRATORS);

    private ProductRoles() {
    }

    public static boolean isProductRoleId(String roleId) {
        return roleId != null && PRODUCT_ROLE_IDS.contains(roleId);
    }

    public static boolean isAdministrator() {
        return hasAny(ADMINISTRATOR_ONLY);
    }

    public static void requireAdministrator() {
        if (!isAdministrator()) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
    }

    public static boolean canWriteVoucher() {
        return hasAny(WRITE_BUSINESS);
    }

    public static boolean canWriteBusiness() {
        return hasAny(WRITE_BUSINESS);
    }

    public static boolean canApproveVoucher() {
        return hasAny(APPROVE_OR_CLOSE);
    }

    public static boolean canClosePeriod() {
        return hasAny(APPROVE_OR_CLOSE);
    }

    public static void requireWriteVoucher() {
        if (!canWriteVoucher()) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
    }

    public static void requireWriteBusiness() {
        if (!canWriteBusiness()) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
    }

    public static void requireApproveVoucher() {
        if (!canApproveVoucher()) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
    }

    public static void requireClosePeriod() {
        if (!canClosePeriod()) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
    }

    private static boolean hasAny(Set<String> wanted) {
        SignedPrincipal principal = AuthorizationUtils.getPrincipal();
        if (principal == null || principal.getAuthorities() == null) {
            return false;
        }
        Collection<? extends Authority> authorities = principal.getAuthorities();
        for (Authority authority : authorities) {
            if (authority != null && wanted.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
