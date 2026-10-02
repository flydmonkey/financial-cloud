package com.financial.cloud.service.voucher;

import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.domain.voucher.VoucherAttachment;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.voucher.VoucherMapper;
import com.financial.cloud.service.auth.FileStorageService;
import com.financial.cloud.service.book.BookSealGuard;
import com.financial.cloud.service.config.ConfigSysService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class VoucherAttachmentIsolationTest {
    @Test
    void ownedAttachmentCannotReadOrDeleteSharedForeignFile() {
        var mapper = mock(VoucherMapper.class);
        var files = mock(FileStorageService.class);
        var service = spy(new VoucherAttachmentService(mapper, files, mock(ConfigSysService.class), mock(BookSealGuard.class)));
        var voucher = new Voucher(); voucher.setId("v"); voucher.setBookId("A");
        var attachment = new VoucherAttachment(); attachment.setId("att"); attachment.setBookId("A");
        attachment.setVoucherId("v"); attachment.setFileId("shared");
        doReturn(attachment).when(service).getById("att");
        when(mapper.selectById("v")).thenReturn(voucher);
        doThrow(new BusinessException(403, "foreign file")).when(files).requireReadable("shared");
        assertThatThrownBy(() -> service.download("att", "A")).hasMessageContaining("foreign file");
        assertThatThrownBy(() -> service.deleteAttachment("att", "A")).hasMessageContaining("foreign file");
        verify(service, never()).removeById("att");
        verify(files, never()).removeById("shared");
    }
}
