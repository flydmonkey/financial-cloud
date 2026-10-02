package com.financial.cloud.service.expense;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.domain.auth.FileStorage;
import com.financial.cloud.domain.expense.ExpenseClaim;
import com.financial.cloud.domain.expense.ExpenseClaimAttachment;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.expense.ExpenseClaimAttachmentMapper;
import com.financial.cloud.repository.expense.ExpenseClaimMapper;
import com.financial.cloud.service.auth.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseClaimAttachmentServiceTest {

    @Test
    void sharedForeignFileCannotBeDeletedThroughOwnedAttachment() {
        var attachment = ExpenseClaimAttachment.builder().id("a-1").bookId(BOOK_ID).claimId("c-1").fileId("foreign-file").build();
        when(attachmentMapper.selectById("a-1")).thenReturn(attachment);
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_DRAFT));
        org.mockito.Mockito.doThrow(new BusinessException(403, "foreign file"))
                .when(fileStorageService).requireReadable("foreign-file");
        assertThrows(BusinessException.class, () -> service.deleteAttachment("a-1", BOOK_ID));
        verify(attachmentMapper, never()).deleteById("a-1");
        verify(fileStorageService, never()).removeById("foreign-file");
    }

    @Test
    void ownedAttachmentCannotDownloadThroughForeignParent() {
        var attachment = ExpenseClaimAttachment.builder().id("a-1").bookId(BOOK_ID).claimId("foreign-claim").fileId("foreign-file").build();
        when(attachmentMapper.selectById("a-1")).thenReturn(attachment);
        var foreign = claim(ExpenseClaim.STATUS_DRAFT); foreign.setBookId("B");
        when(expenseClaimMapper.selectById("foreign-claim")).thenReturn(foreign);
        assertThrows(BusinessException.class, () -> service.download("a-1", BOOK_ID));
        verify(fileStorageService, never()).requireReadable(any());
    }

    private static final String BOOK_ID = "book-test-1";

    @Mock
    private ExpenseClaimAttachmentMapper attachmentMapper;
    @Mock
    private ExpenseClaimMapper expenseClaimMapper;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ExpenseClaimAttachmentService service;

    private UserInfo operator() {
        UserInfo user = new UserInfo();
        user.setBookId(BOOK_ID);
        user.setUsername("tester");
        return user;
    }

    private ExpenseClaim claim(String status) {
        return ExpenseClaim.builder()
                .id("c-1")
                .bookId(BOOK_ID)
                .claimStatus(status)
                .build();
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "发票.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThrows(BusinessException.class, () -> service.upload(empty, "c-1", operator()));
    }

    @Test
    void uploadRejectsUnsupportedType() {
        MockMultipartFile exe = new MockMultipartFile("file", "a.exe", "application/octet-stream", new byte[]{1});

        assertThrows(BusinessException.class, () -> service.upload(exe, "c-1", operator()));
    }

    @Test
    void uploadRejectsApprovedClaim() {
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_APPROVED));

        assertThrows(BusinessException.class, () -> service.upload(png(), "c-1", operator()));
        verify(fileStorageService, never()).save(any(FileStorage.class));
    }

    @Test
    void uploadStoresFileAndInsertsAttachment() {
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_DRAFT));
        when(attachmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ExpenseClaimAttachment saved = service.upload(png(), "c-1", operator());

        ArgumentCaptor<FileStorage> storageCaptor = ArgumentCaptor.forClass(FileStorage.class);
        verify(fileStorageService).save(storageCaptor.capture());
        assertEquals("expense", storageCaptor.getValue().getCategory());
        assertEquals("发票.png", storageCaptor.getValue().getFileName());

        ArgumentCaptor<ExpenseClaimAttachment> captor = ArgumentCaptor.forClass(ExpenseClaimAttachment.class);
        verify(attachmentMapper).insert(captor.capture());
        assertEquals("c-1", captor.getValue().getClaimId());
        assertEquals(1, captor.getValue().getSortIndex());
        assertEquals("image/png", captor.getValue().getContentType());
        assertEquals("发票.png", saved.getFileName());
    }

    @Test
    void deleteRemovesStorageWithAttachment() {
        ExpenseClaimAttachment attachment = ExpenseClaimAttachment.builder()
                .id("a-1")
                .bookId(BOOK_ID)
                .claimId("c-1")
                .fileId("f-1")
                .build();
        when(attachmentMapper.selectById("a-1")).thenReturn(attachment);
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_DRAFT));

        service.deleteAttachment("a-1", BOOK_ID);

        verify(attachmentMapper).deleteById("a-1");
        verify(fileStorageService).removeById("f-1");
    }

    @Test
    void deleteRejectsApprovedClaim() {
        ExpenseClaimAttachment attachment = ExpenseClaimAttachment.builder()
                .id("a-1")
                .bookId(BOOK_ID)
                .claimId("c-1")
                .fileId("f-1")
                .build();
        when(attachmentMapper.selectById("a-1")).thenReturn(attachment);
        when(expenseClaimMapper.selectById("c-1")).thenReturn(claim(ExpenseClaim.STATUS_APPROVED));

        assertThrows(BusinessException.class, () -> service.deleteAttachment("a-1", BOOK_ID));
        verify(fileStorageService, never()).removeById(any(String.class));
    }

    @Test
    void downloadRejectsMissingAttachment() {
        when(attachmentMapper.selectById("a-x")).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.download("a-x", BOOK_ID));
    }
}
