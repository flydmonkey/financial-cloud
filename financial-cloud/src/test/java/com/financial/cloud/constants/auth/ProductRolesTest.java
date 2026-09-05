package com.financial.cloud.constants.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductRolesTest {

    @Test
    void productRoleIdsAreRecognized() {
        assertTrue(ProductRoles.isProductRoleId(ProductRoles.ADMINISTRATORS));
        assertTrue(ProductRoles.isProductRoleId(ProductRoles.BOOKKEEPER));
        assertTrue(ProductRoles.isProductRoleId(ProductRoles.REVIEWER));
        assertTrue(ProductRoles.isProductRoleId(ProductRoles.VIEWER));
        assertFalse(ProductRoles.isProductRoleId("1880191154616516610"));
        assertFalse(ProductRoles.isProductRoleId("ROLE_SUPERVISOR"));
        assertFalse(ProductRoles.isProductRoleId("1000"));
        assertFalse(ProductRoles.isProductRoleId(null));
    }
}
