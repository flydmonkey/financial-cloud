package com.financial.cloud.service.config;

import com.financial.cloud.common.Message;
import com.financial.cloud.constants.system.ConstsSysConfig;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.config.ConfigSys;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.config.VoucherSettlementParamsSaveDto;
import com.financial.cloud.dto.config.VoucherSettlementParamsVo;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.book.BookSealGuard;
import com.financial.cloud.service.book.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherSettlementParamsServiceTest {

    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookService bookService;
    @Mock
    private BookSealGuard bookSealGuard;
    @Mock
    private ConfigSysService configSysService;

    @InjectMocks
    private VoucherSettlementParamsService service;

    private static UserInfo user() {
        UserInfo user = new UserInfo();
        user.setId("user-1");
        user.setBookId("book-1");
        return user;
    }

    @Test
    void get_blankArapConfigMeansEnabled() {
        Book book = new Book();
        book.setId("book-1");
        book.setVoucherReviewed(1);
        when(bookMapper.selectById("book-1")).thenReturn(book);
        when(bookService.isBookAdministrator(any(), eq("book-1"))).thenReturn(false);
        when(configSysService.selectConfigByKey("book-1", ConstsSysConfig.SYS_SETTLEMENT_ARAP_VERIFY))
                .thenReturn("");

        VoucherSettlementParamsVo vo = service.get(user());

        assertEquals(1, vo.getVoucherReviewed());
        assertTrue(vo.isArapVerifyEnabled());
        assertFalse(vo.isCanEdit());
        assertEquals(5, vo.getHardGateLabels().size());
        verify(configSysService).ensureBookConfigsComplete("book-1");
    }

    @Test
    void save_rejectsNonAdmin() {
        doThrow(new BusinessException(UsersBusinessCode.PERMISSION_DENIED))
                .when(bookService).requireBookAdministrator(any(), eq("book-1"));
        VoucherSettlementParamsSaveDto dto = new VoucherSettlementParamsSaveDto();
        dto.setVoucherReviewed(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(dto, user()));
        assertEquals(UsersBusinessCode.PERMISSION_DENIED.getCode(), ex.getCode());
        verify(bookMapper, never()).updateById(any(Book.class));
        verify(configSysService, never()).update(any());
    }

    @Test
    void save_updatesReviewAndArap() {
        VoucherSettlementParamsSaveDto dto = new VoucherSettlementParamsSaveDto();
        dto.setVoucherReviewed(0);
        dto.setArapVerifyEnabled(false);

        Message<String> result = service.save(dto, user());

        assertEquals(Message.SUCCESS, result.getCode());
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookMapper).updateById(bookCaptor.capture());
        assertEquals("book-1", bookCaptor.getValue().getId());
        assertEquals(0, bookCaptor.getValue().getVoucherReviewed());
        ArgumentCaptor<ConfigSys> cfgCaptor = ArgumentCaptor.forClass(ConfigSys.class);
        verify(configSysService).update(cfgCaptor.capture());
        assertEquals("false", cfgCaptor.getValue().getConfigValue());
        verify(bookSealGuard).assertWritable("book-1");
    }
}
