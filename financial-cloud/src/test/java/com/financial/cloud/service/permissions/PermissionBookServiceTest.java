package com.financial.cloud.service.permissions;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.financial.cloud.repository.permissions.PermissionBookMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class PermissionBookServiceTest {

    @Mock
    private PermissionBookMapper permissionBookMapper;

    @Test
    void userHasBookRequiresGrant() {
        PermissionBookService service = spy(new PermissionBookService(permissionBookMapper));
        doReturn(1L).when(service).count(any(Wrapper.class));
        assertTrue(service.userHasBook("u1", "b1"));

        doReturn(0L).when(service).count(any(Wrapper.class));
        assertFalse(service.userHasBook("u1", "b2"));
    }

    @Test
    void userHasBookRejectsBlank() {
        PermissionBookService service = spy(new PermissionBookService(permissionBookMapper));
        assertFalse(service.userHasBook(null, "b1"));
        assertFalse(service.userHasBook("u1", ""));
    }
}
