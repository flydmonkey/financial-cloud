package com.financial.cloud.service.book;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.book.BookPageDto;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.config.ConfigCashFlowBalanceService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.standard.StandardSubjectCashFlowService;
import com.financial.cloud.service.statement.StatementBalanceSheetService;
import com.financial.cloud.service.statement.StatementIncomeService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.service.voucher.VoucherTemplateService;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private IdentifierGenerator identifierGenerator;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookSubjectService bookSubjectService;
    @Mock
    private ConfigCashFlowBalanceService configCashFlowBalanceService;
    @Mock
    private StatementIncomeService statementIncomeService;
    @Mock
    private StatementBalanceSheetService statementBalanceSheetService;
    @Mock
    private ConfigSysService configSysService;
    @Mock
    private VoucherService voucherService;
    @Mock
    private VoucherTemplateService voucherTemplateService;
    @Mock
    private StandardSubjectCashFlowService standardSubjectCashFlowService;
    @Mock
    private com.financial.cloud.service.permissions.PermissionBookService permissionBookService;
    @Mock
    private com.financial.cloud.service.idm.RoleMemberService roleMemberService;
    @Mock
    private com.financial.cloud.service.config.ConfigInsuranceFundService configInsuranceFundService;

    @InjectMocks
    private BookService bookService;

    @Test
    void pageList_returnsPagedBooksForCurrentUser() {
        BookPageDto dto = new BookPageDto();
        dto.setPageNumber(1);
        dto.setPageSize(10);

        Page<Book> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(new Book()));
        page.setTotal(1);

        when(bookMapper.pageList(any(), any())).thenReturn(page);

        Message<Page<Book>> result = bookService.pageList(dto, "user-1");

        assertEquals("user-1", dto.getUserId());
        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void requireBookAdministrator_deniesWhenNotAdmin() {
        UserInfo user = new UserInfo();
        user.setId("user-2");
        when(roleMemberService.count(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.requireBookAdministrator(user, "book-1"));
        assertEquals(UsersBusinessCode.PERMISSION_DENIED.getCode(), ex.getCode());
    }

    @Test
    void requireBookAdministrator_allowsWhenAdmin() {
        UserInfo user = new UserInfo();
        user.setId("user-1");
        when(roleMemberService.count(any())).thenReturn(1L);
        bookService.requireBookAdministrator(user, "book-1");
    }
}
