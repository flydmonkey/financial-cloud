package com.financial.cloud.service.fixedasset;

import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.voucher.VoucherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedAssetServiceSurplusVoucherTest {

    private static final String BOOK_ID = "book-1";

    @Mock
    private com.financial.cloud.repository.fixedasset.FixedAssetMapper fixedAssetMapper;
    @Mock
    private com.financial.cloud.repository.fixedasset.FixedAssetDeprMapper fixedAssetDeprMapper;
    @Mock
    private com.financial.cloud.repository.fixedasset.AssetCategoryMapper assetCategoryMapper;
    @Mock
    private com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator identifierGenerator;
    @Mock
    private ConfigSysService configSysService;
    @Mock
    private BookSubjectService bookSubjectService;
    @Mock
    private FixedAssetChangeService fixedAssetChangeService;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private VoucherService voucherService;
    @Mock
    private com.financial.cloud.service.idm.OrganizationsService organizationsService;

    @InjectMocks
    private FixedAssetService service;

    @Test
    void createSurplusVoucher_debitFixedAssetCreditSurplusGain() {
        FixedAsset asset = asset();
        BookSubject fa = subject("s-1601", "1601", "固定资产");
        BookSubject gain = subject("s-530104", "5301.04", "盘盈利得");
        when(bookSubjectService.selectSubject(BOOK_ID, "1601")).thenReturn(fa);
        when(bookSubjectService.selectSubject(BOOK_ID, "5301.04")).thenReturn(gain);
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2026-09");
        when(voucherService.getAbleWordNum(anyString(), anyString(), any(), any())).thenReturn(Message.ok(7));
        when(voucherService.save(any(VoucherChangeDto.class))).thenReturn(Message.ok("v-1"));

        String id = service.createSurplusVoucher(asset, new BigDecimal("500"), "盘盈：9月盘点");

        assertEquals("v-1", id);
        ArgumentCaptor<VoucherChangeDto> captor = ArgumentCaptor.forClass(VoucherChangeDto.class);
        verify(voucherService).save(captor.capture());
        VoucherChangeDto dto = captor.getValue();
        assertEquals(VoucherStatusEnum.DRAFT.getValue(), dto.getStatus());
        assertEquals(2, dto.getItems().size());
        assertEquals("s-1601", dto.getItems().get(0).getSubjectId());
        assertEquals(0, new BigDecimal("500.00").compareTo(dto.getItems().get(0).getDebitAmount()));
        assertEquals("s-530104", dto.getItems().get(1).getSubjectId());
        assertEquals(0, new BigDecimal("500.00").compareTo(dto.getItems().get(1).getCreditAmount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(dto.getDebitAmount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(dto.getCreditAmount()));
    }

    @Test
    void createSurplusVoucher_fallsBackTo5301() {
        FixedAsset asset = asset();
        BookSubject fa = subject("s-1601", "1601", "固定资产");
        BookSubject gain = subject("s-5301", "5301", "营业外收入");
        when(bookSubjectService.selectSubject(BOOK_ID, "1601")).thenReturn(fa);
        when(bookSubjectService.selectSubject(BOOK_ID, "5301.04")).thenReturn(null);
        when(bookSubjectService.selectSubject(BOOK_ID, "5301")).thenReturn(gain);
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2026-09");
        when(voucherService.getAbleWordNum(anyString(), anyString(), any(), any())).thenReturn(Message.ok(1));
        when(voucherService.save(any(VoucherChangeDto.class))).thenReturn(Message.ok("v-2"));

        assertEquals("v-2", service.createSurplusVoucher(asset, new BigDecimal("10"), null));

        ArgumentCaptor<VoucherChangeDto> captor = ArgumentCaptor.forClass(VoucherChangeDto.class);
        verify(voucherService).save(captor.capture());
        assertEquals("s-5301", captor.getValue().getItems().get(1).getSubjectId());
    }

    @Test
    void createSurplusVoucher_missingGainSubjectRejected() {
        FixedAsset asset = asset();
        when(bookSubjectService.selectSubject(BOOK_ID, "1601")).thenReturn(subject("s-1601", "1601", "固定资产"));
        when(bookSubjectService.selectSubject(BOOK_ID, "5301.04")).thenReturn(null);
        when(bookSubjectService.selectSubject(BOOK_ID, "5301")).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.createSurplusVoucher(asset, new BigDecimal("10"), "盘盈"));
        verify(voucherService, never()).save(any(VoucherChangeDto.class));
    }

    @Test
    void createSurplusVoucher_nonPositiveAmountRejected() {
        FixedAsset asset = asset();
        assertThrows(BusinessException.class, () -> service.createSurplusVoucher(asset, BigDecimal.ZERO, "盘盈"));
        assertThrows(BusinessException.class, () -> service.createSurplusVoucher(asset, null, "盘盈"));
        verify(voucherService, never()).save(any(VoucherChangeDto.class));
    }

    private FixedAsset asset() {
        FixedAsset a = new FixedAsset();
        a.setId("asset-1");
        a.setBookId(BOOK_ID);
        a.setCode("FA-001");
        a.setName("电脑");
        return a;
    }

    private BookSubject subject(String id, String code, String name) {
        BookSubject s = new BookSubject();
        s.setId(id);
        s.setBookId(BOOK_ID);
        s.setCode(code);
        s.setName(name);
        return s;
    }
}
