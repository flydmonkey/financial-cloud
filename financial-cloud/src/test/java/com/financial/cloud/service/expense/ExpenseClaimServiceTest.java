package com.financial.cloud.service.expense;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.expense.ExpenseClaim;
import com.financial.cloud.domain.expense.ExpenseClaimItem;
import com.financial.cloud.dto.expense.ExpenseClaimItemDto;
import com.financial.cloud.dto.expense.ExpenseClaimSaveDto;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.expense.ExpenseClaimItemMapper;
import com.financial.cloud.repository.expense.ExpenseClaimMapper;
import com.financial.cloud.service.voucher.VoucherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseClaimServiceTest {

    private static final String BOOK_ID = "book-test-1";

    @Mock
    private ExpenseClaimMapper expenseClaimMapper;
    @Mock
    private ExpenseClaimItemMapper expenseClaimItemMapper;
    @Mock
    private BookSubjectMapper bookSubjectMapper;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private ExpenseClaimService service;

    private ExpenseClaim claim(String status) {
        return ExpenseClaim.builder()
                .id("c-1")
                .bookId(BOOK_ID)
                .claimNo("BX202609-0001")
                .claimant("张三")
                .claimDate(LocalDate.of(2026, 9, 15))
                .expenseSubjectCode("6602")
                .fundSubjectCode("1001")
                .amount(new BigDecimal("800.00"))
                .summary("差旅费")
                .claimStatus(status)
                .build();
    }

    private ExpenseClaimItem item(String code, String name, String amount, String summary) {
        return ExpenseClaimItem.builder()
                .claimId("c-1")
                .expenseSubjectCode(code)
                .expenseSubjectName(code + "-" + name)
                .amount(new BigDecimal(amount))
                .summary(summary)
                .build();
    }

    private BookSubject subject(String code, String name) {
        BookSubject s = new BookSubject();
        s.setId("sub-" + code);
        s.setCode(code);
        s.setName(name);
        return s;
    }

    private ExpenseClaimItemDto line(String subjectCode, String amount) {
        ExpenseClaimItemDto dto = new ExpenseClaimItemDto();
        dto.setExpenseSubjectCode(subjectCode);
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }

    private ExpenseClaimSaveDto saveDto(ExpenseClaimItemDto... lines) {
        ExpenseClaimSaveDto dto = new ExpenseClaimSaveDto();
        dto.setClaimant("张三");
        dto.setClaimDate("2026-09-15");
        dto.setFundSubjectCode("1001");
        dto.setSummary("差旅费");
        dto.setItems(List.of(lines));
        return dto;
    }

    @Test
    void saveRejectsEmptyItems() {
        ExpenseClaimSaveDto dto = saveDto();
        dto.setItems(List.of());

        assertThrows(BusinessException.class, () -> service.save(BOOK_ID, dto));
    }

    @Test
    void saveRejectsNonPositiveLineAmount() {
        assertThrows(BusinessException.class, () -> service.save(BOOK_ID, saveDto(line("6602", "0"))));
    }

    @Test
    void saveCreatesDraftWithSummedAmountAndItems() {
        when(bookSubjectMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subject("1001", "库存现金"), subject("6602", "管理费用"), subject("6603", "销售费用"));
        when(expenseClaimMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        Message<String> msg = service.save(BOOK_ID, saveDto(line("6602", "500"), line("6603", "300")));

        assertEquals(Message.SUCCESS, msg.getCode());
        ArgumentCaptor<ExpenseClaim> claimCaptor = ArgumentCaptor.forClass(ExpenseClaim.class);
        verify(expenseClaimMapper).insert(claimCaptor.capture());
        assertEquals("BX202609-0003", claimCaptor.getValue().getClaimNo());
        assertEquals(ExpenseClaim.STATUS_DRAFT, claimCaptor.getValue().getClaimStatus());
        assertEquals(new BigDecimal("800.00"), claimCaptor.getValue().getAmount());
        assertEquals("6602-管理费用 等2项", claimCaptor.getValue().getExpenseSubjectName());
        verify(expenseClaimItemMapper, times(2)).insert(any(ExpenseClaimItem.class));
    }

    @Test
    void submitTransitionsDraftToSubmitted() {
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_DRAFT));

        service.submit("c-1", BOOK_ID);

        ArgumentCaptor<ExpenseClaim> captor = ArgumentCaptor.forClass(ExpenseClaim.class);
        verify(expenseClaimMapper).updateById(captor.capture());
        assertEquals(ExpenseClaim.STATUS_SUBMITTED, captor.getValue().getClaimStatus());
    }

    @Test
    void submitRejectsApprovedClaim() {
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_APPROVED));

        assertThrows(BusinessException.class, () -> service.submit("c-1", BOOK_ID));
    }

    @Test
    void auditRejectRequiresReason() {
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_SUBMITTED));

        assertThrows(BusinessException.class, () -> service.audit("c-1", BOOK_ID, false, "", "李四"));
    }

    @Test
    void auditApproveSetsAuditorAndStatus() {
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_SUBMITTED));

        service.audit("c-1", BOOK_ID, true, "", "李四");

        ArgumentCaptor<ExpenseClaim> captor = ArgumentCaptor.forClass(ExpenseClaim.class);
        verify(expenseClaimMapper).updateById(captor.capture());
        assertEquals(ExpenseClaim.STATUS_APPROVED, captor.getValue().getClaimStatus());
        assertEquals("李四", captor.getValue().getAuditBy());
    }

    @Test
    void generateVoucherCreatesDebitPerLineAndSingleCredit() {
        ExpenseClaim approved = claim(ExpenseClaim.STATUS_APPROVED);
        when(expenseClaimMapper.selectById("c-1")).thenReturn(approved);
        when(expenseClaimItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                item("6602", "管理费用", "500.00", "市内交通"),
                item("6603", "销售费用", "300.00", "客户招待")));
        when(bookSubjectMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subject("1001", "库存现金"), subject("6602", "管理费用"), subject("6603", "销售费用"));
        Book book = new Book();
        book.setCompanyName("测试公司");
        when(bookMapper.selectById(BOOK_ID)).thenReturn(book);
        when(voucherService.getAbleWordNum(eq(BOOK_ID), eq("记"), anyInt(), anyInt()))
                .thenReturn(Message.ok(7));
        when(voucherService.save(any(VoucherChangeDto.class))).thenReturn(Message.ok("v-1"));

        Message<String> msg = service.generateVoucher("c-1", BOOK_ID);

        assertEquals("v-1", msg.getData());
        ArgumentCaptor<VoucherChangeDto> captor = ArgumentCaptor.forClass(VoucherChangeDto.class);
        verify(voucherService).save(captor.capture());
        VoucherChangeDto dto = captor.getValue();
        assertEquals(new BigDecimal("800.00"), dto.getDebitAmount());
        assertEquals(3, dto.getItems().size());
        assertEquals(new BigDecimal("500.00"), dto.getItems().get(0).getDebitAmount());
        assertEquals("6602", dto.getItems().get(0).getSubjectCode());
        assertEquals(new BigDecimal("300.00"), dto.getItems().get(1).getDebitAmount());
        assertEquals("6603", dto.getItems().get(1).getSubjectCode());
        assertEquals(new BigDecimal("800.00"), dto.getItems().get(2).getCreditAmount());
        assertEquals("1001", dto.getItems().get(2).getSubjectCode());
        assertEquals("v-1", approved.getVoucherId());
    }

    @Test
    void generateVoucherIsIdempotent() {
        ExpenseClaim approved = claim(ExpenseClaim.STATUS_APPROVED);
        approved.setVoucherId("v-9");
        when(expenseClaimMapper.selectById("c-1")).thenReturn(approved);

        Message<String> msg = service.generateVoucher("c-1", BOOK_ID);

        assertEquals("v-9", msg.getData());
        verify(voucherService, never()).save(any(VoucherChangeDto.class));
    }

    @Test
    void deleteRemovesItemsWithClaim() {
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_DRAFT));

        service.delete("c-1", BOOK_ID);

        verify(expenseClaimItemMapper).delete(any(LambdaQueryWrapper.class));
        verify(expenseClaimMapper).deleteById("c-1");
    }

    @Test
    void deleteRejectsApprovedClaim() {
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_APPROVED));

        assertThrows(BusinessException.class, () -> service.delete("c-1", BOOK_ID));
        verify(expenseClaimMapper, never()).deleteById(anyString());
    }
}
