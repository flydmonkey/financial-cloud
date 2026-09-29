package com.financial.cloud.service.fixedasset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.domain.fixedasset.FixedAssetCheck;
import com.financial.cloud.domain.fixedasset.FixedAssetCheckItem;
import com.financial.cloud.dto.fixedasset.FixedAssetCheckDtos;
import com.financial.cloud.enums.fixedasset.FixedAssetStatus;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.fixedasset.FixedAssetCheckItemMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetCheckMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetMapper;
import com.financial.cloud.service.book.BookSealGuard;
import com.financial.cloud.service.config.ConfigSysService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedAssetCheckServiceTest {

    private static final String BOOK_ID = "book-test-1";

    @Mock
    private FixedAssetCheckMapper checkMapper;
    @Mock
    private FixedAssetCheckItemMapper itemMapper;
    @Mock
    private FixedAssetMapper assetMapper;
    @Mock
    private BookSealGuard bookSealGuard;
    @Mock
    private FixedAssetService fixedAssetService;
    @Mock
    private com.financial.cloud.service.config.ConfigSysService configSysService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private FixedAssetCheckService service;

    @Test
    void create_snapshotsOnHandAssets() {
        FixedAsset a1 = new FixedAsset();
        a1.setId("asset-1");
        a1.setCode("FA-001");
        a1.setName("电脑");
        a1.setQuantity(2);
        a1.setStatus(FixedAssetStatus.IN_USE.name());
        FixedAsset a2 = new FixedAsset();
        a2.setId("asset-2");
        a2.setCode("FA-002");
        a2.setName("打印机");
        a2.setStatus(FixedAssetStatus.IN_USE.name());
        when(assetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a1, a2));
        // 雪花 ID 由 MyBatis-Plus 在 insert 时回填，单测里模拟回填
        doAnswer(inv -> {
            FixedAssetCheck c = inv.getArgument(0);
            c.setId("check-1");
            return 1;
        }).when(checkMapper).insert(any(FixedAssetCheck.class));

        FixedAssetCheckDtos.CreateDto dto = new FixedAssetCheckDtos.CreateDto();
        dto.setTitle("9月盘点");
        FixedAssetCheck check = service.create(dto, BOOK_ID);

        assertEquals(FixedAssetCheck.STATUS_DRAFT, check.getStatus());
        assertEquals(2, check.getTotalCount());
        verify(checkMapper).insert(any(FixedAssetCheck.class));
        verify(itemMapper, times(2)).insert(any(FixedAssetCheckItem.class));
    }

    @Test
    void create_blankTitleRejected() {
        FixedAssetCheckDtos.CreateDto dto = new FixedAssetCheckDtos.CreateDto();
        dto.setTitle(" ");
        assertThrows(BusinessException.class, () -> service.create(dto, BOOK_ID));
    }

    @Test
    void updateItem_marksResultImmediately() {
        FixedAssetCheck check = draftCheck();
        FixedAssetCheckItem item = item("item-1", 2);
        when(itemMapper.selectById("item-1")).thenReturn(item);
        when(checkMapper.selectById("check-1")).thenReturn(check);

        FixedAssetCheckDtos.ItemDto dto = new FixedAssetCheckDtos.ItemDto();
        dto.setId("item-1");
        dto.setActualQuantity(1);
        FixedAssetCheckItem updated = service.updateItem(dto, BOOK_ID);

        assertEquals(FixedAssetCheckItem.RESULT_DEFICIT, updated.getResult());
        verify(itemMapper).updateById(item);
    }

    @Test
    void updateItem_negativeQuantityRejected() {
        FixedAssetCheck check = draftCheck();
        FixedAssetCheckItem item = item("item-1", 2);
        when(itemMapper.selectById("item-1")).thenReturn(item);
        when(checkMapper.selectById("check-1")).thenReturn(check);

        FixedAssetCheckDtos.ItemDto dto = new FixedAssetCheckDtos.ItemDto();
        dto.setId("item-1");
        dto.setActualQuantity(-1);
        assertThrows(BusinessException.class, () -> service.updateItem(dto, BOOK_ID));
    }

    @Test
    void updateItem_completedCheckRejected() {
        FixedAssetCheck check = draftCheck();
        check.setStatus(FixedAssetCheck.STATUS_COMPLETED);
        FixedAssetCheckItem item = item("item-1", 2);
        when(itemMapper.selectById("item-1")).thenReturn(item);
        when(checkMapper.selectById("check-1")).thenReturn(check);

        FixedAssetCheckDtos.ItemDto dto = new FixedAssetCheckDtos.ItemDto();
        dto.setId("item-1");
        dto.setActualQuantity(1);
        assertThrows(BusinessException.class, () -> service.updateItem(dto, BOOK_ID));
    }

    @Test
    void complete_requiresAllActualQuantities() {
        FixedAssetCheck check = draftCheck();
        when(checkMapper.selectById("check-1")).thenReturn(check);
        FixedAssetCheckItem missing = item("item-1", 2);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(missing));

        assertThrows(BusinessException.class, () -> service.complete("check-1", BOOK_ID));
    }

    @Test
    void complete_summarizesResults() {
        FixedAssetCheck check = draftCheck();
        when(checkMapper.selectById("check-1")).thenReturn(check);
        FixedAssetCheckItem normal = item("item-1", 2);
        normal.setActualQuantity(2);
        FixedAssetCheckItem surplus = item("item-2", 1);
        surplus.setActualQuantity(3);
        FixedAssetCheckItem deficit = item("item-3", 2);
        deficit.setActualQuantity(0);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(normal, surplus, deficit));

        FixedAssetCheck done = service.complete("check-1", BOOK_ID);

        assertEquals(FixedAssetCheck.STATUS_COMPLETED, done.getStatus());
        assertEquals(1, done.getNormalCount());
        assertEquals(1, done.getSurplusCount());
        assertEquals(1, done.getDeficitCount());
        verify(checkMapper).updateById(check);
    }

    @Test
    void delete_completedCheckRejected() {
        FixedAssetCheck check = draftCheck();
        check.setStatus(FixedAssetCheck.STATUS_COMPLETED);
        when(checkMapper.selectById("check-1")).thenReturn(check);

        assertThrows(BusinessException.class, () -> service.delete("check-1", BOOK_ID));
    }

    @Test
    void resolveResult_rules() {
        assertEquals(FixedAssetCheckItem.RESULT_NORMAL, FixedAssetCheckService.resolveResult(2, 2));
        assertEquals(FixedAssetCheckItem.RESULT_SURPLUS, FixedAssetCheckService.resolveResult(1, 2));
        assertEquals(FixedAssetCheckItem.RESULT_DEFICIT, FixedAssetCheckService.resolveResult(2, 1));
        assertEquals(FixedAssetCheckItem.RESULT_DEFICIT, FixedAssetCheckService.resolveResult(1, null));
    }

    @Test
    void disposeDeficit_rejectsDraftCheck() {
        when(checkMapper.selectById("check-1")).thenReturn(draftCheck());
        assertThrows(BusinessException.class, () -> service.disposeDeficit("check-1", BOOK_ID));
    }

    @Test
    void disposeDeficit_processesFullDeficitSkipsPartialAndSurplus() {
        FixedAssetCheck check = draftCheck();
        check.setStatus(FixedAssetCheck.STATUS_COMPLETED);
        when(checkMapper.selectById("check-1")).thenReturn(check);

        FixedAssetCheckItem fullDeficit = item("item-1", 1);
        fullDeficit.setActualQuantity(0);
        fullDeficit.setResult(FixedAssetCheckItem.RESULT_DEFICIT);
        FixedAssetCheckItem partialDeficit = item("item-2", 2);
        partialDeficit.setActualQuantity(1);
        partialDeficit.setResult(FixedAssetCheckItem.RESULT_DEFICIT);
        FixedAssetCheckItem surplus = item("item-3", 1);
        surplus.setActualQuantity(2);
        surplus.setResult(FixedAssetCheckItem.RESULT_SURPLUS);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(fullDeficit, partialDeficit, surplus));
        when(fixedAssetService.dispose(eq("asset-x"), eq(BOOK_ID), any()))
                .thenReturn(new com.financial.cloud.common.Message<>(
                        com.financial.cloud.common.Message.SUCCESS, "清理成功"));

        FixedAssetCheckDtos.DeficitDisposeVo vo = service.disposeDeficit("check-1", BOOK_ID);

        assertEquals(1, vo.getProcessedCount());
        assertEquals(1, vo.getSurplusCount());
        assertEquals(1, vo.getSkipped().size());
        assertEquals(true, vo.getSkipped().get(0).getReason().contains("部分盘亏"));
    }

    @Test
    void disposeDeficit_collectsFailures() {
        FixedAssetCheck check = draftCheck();
        check.setStatus(FixedAssetCheck.STATUS_COMPLETED);
        when(checkMapper.selectById("check-1")).thenReturn(check);

        FixedAssetCheckItem fullDeficit = item("item-1", 1);
        fullDeficit.setActualQuantity(0);
        fullDeficit.setResult(FixedAssetCheckItem.RESULT_DEFICIT);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(fullDeficit));
        when(fixedAssetService.dispose(eq("asset-x"), eq(BOOK_ID), any()))
                .thenThrow(new BusinessException(400, "该资产已清理"));

        FixedAssetCheckDtos.DeficitDisposeVo vo = service.disposeDeficit("check-1", BOOK_ID);

        assertEquals(0, vo.getProcessedCount());
        assertEquals(1, vo.getSkipped().size());
        assertEquals("该资产已清理", vo.getSkipped().get(0).getReason());
    }

    @Test
    void defaultSurplusAmount_proratesOriginalValue() {
        assertEquals(new BigDecimal("500.00"),
                FixedAssetCheckService.defaultSurplusAmount(new BigDecimal("1000"), 2, 3));
    }

    @Test
    void defaultSurplusAmount_zeroWhenNoIncrease() {
        assertEquals(0, FixedAssetCheckService.defaultSurplusAmount(new BigDecimal("1000"), 1, 1).compareTo(BigDecimal.ZERO));
    }

    @Test
    void surplusPreview_rejectsDraftCheck() {
        when(checkMapper.selectById("check-1")).thenReturn(draftCheck());
        assertThrows(BusinessException.class, () -> service.surplusPreview("check-1", BOOK_ID));
    }

    @Test
    void surplusPreview_listsUnbookedSurplusWithStrategy() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem single = surplusItem("item-1", "asset-1", 1, 2);
        FixedAssetCheckItem multi = surplusItem("item-2", "asset-2", 2, 3);
        FixedAssetCheckItem booked = surplusItem("item-3", "asset-3", 1, 2);
        booked.setSurplusVoucherId("v-1");
        FixedAssetCheckItem normal = item("item-4", 1);
        normal.setActualQuantity(1);
        normal.setResult(FixedAssetCheckItem.RESULT_NORMAL);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(single, multi, booked, normal));
        when(assetMapper.selectById("asset-1")).thenReturn(asset("asset-1", "FA-1", 1, "1000"));
        when(assetMapper.selectById("asset-2")).thenReturn(asset("asset-2", "FA-2", 2, "1000"));

        FixedAssetCheckDtos.SurplusPreviewVo vo = service.surplusPreview("check-1", BOOK_ID);

        assertEquals(2, vo.getRows().size());
        FixedAssetCheckDtos.SurplusPreviewRow r1 = vo.getRows().get(0);
        assertEquals("split_card", r1.getStrategy());
        assertEquals(1, r1.getSurplusQuantity());
        assertEquals(new BigDecimal("1000.00"), r1.getDefaultAmount());
        FixedAssetCheckDtos.SurplusPreviewRow r2 = vo.getRows().get(1);
        assertEquals("bump_qty", r2.getStrategy());
        assertEquals(new BigDecimal("500.00"), r2.getDefaultAmount());
        assertFalse(r2.isHasDepreciation());
        assertEquals(null, r2.getWarning());
    }

    @Test
    void surplusPreview_warnsWhenBumpQtyHasDepreciation() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem multi = surplusItem("item-2", "asset-2", 2, 3);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(multi));
        FixedAsset asset = asset("asset-2", "FA-2", 2, "1000");
        asset.setAccumDepr(new BigDecimal("100"));
        asset.setDepreciatedPeriods(3);
        when(assetMapper.selectById("asset-2")).thenReturn(asset);

        FixedAssetCheckDtos.SurplusPreviewVo vo = service.surplusPreview("check-1", BOOK_ID);

        assertEquals(1, vo.getRows().size());
        assertTrue(vo.getRows().get(0).isHasDepreciation());
        assertTrue(vo.getRows().get(0).getWarning() != null
                && vo.getRows().get(0).getWarning().contains("不允许在原卡累加数量入账"));
    }

    @Test
    void hasExistingDepreciation_trueWhenAccumOrPeriods() {
        FixedAsset a = new FixedAsset();
        assertFalse(FixedAssetCheckService.hasExistingDepreciation(a));
        a.setAccumDepr(new BigDecimal("0.01"));
        assertTrue(FixedAssetCheckService.hasExistingDepreciation(a));
        a.setAccumDepr(BigDecimal.ZERO);
        a.setDepreciatedPeriods(1);
        assertTrue(FixedAssetCheckService.hasExistingDepreciation(a));
    }

    @Test
    void page_fillsPendingSurplusCount() {
        FixedAssetCheckDtos.PageDto dto = new FixedAssetCheckDtos.PageDto();
        dto.setBookId(BOOK_ID);
        dto.setPageNumber(1);
        dto.setPageSize(10);
        FixedAssetCheck completed = completedCheck();
        completed.setSurplusCount(2);
        Page<FixedAssetCheck> page = new Page<>(1, 10);
        page.setRecords(List.of(completed));
        page.setTotal(1);
        when(checkMapper.selectPage(any(), any())).thenReturn(page);
        when(itemMapper.selectCount(any())).thenReturn(1L);

        Page<FixedAssetCheck> result = service.page(dto);

        assertEquals(1, result.getRecords().get(0).getPendingSurplusCount());
        verify(itemMapper).selectCount(any());
    }

    @Test
    void bookSurplus_skipsAlreadyBooked() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem booked = surplusItem("item-1", "asset-1", 1, 2);
        booked.setSurplusVoucherId("v-1");
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(booked));
        when(itemMapper.selectById("item-1")).thenReturn(booked);

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "800")));

        assertEquals(0, vo.getProcessedCount());
        assertEquals(1, vo.getSkipped().size());
        verify(fixedAssetService, never()).createSurplusVoucher(any(), any(), any());
    }

    @Test
    void bookSurplus_skipsWhenItemBookedConcurrentlyAfterListing() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem stale = surplusItem("item-1", "asset-1", 1, 2);
        FixedAssetCheckItem fresh = surplusItem("item-1", "asset-1", 1, 2);
        fresh.setSurplusVoucherId("v-other");
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(stale));
        when(itemMapper.selectById("item-1")).thenReturn(fresh);

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "800")));

        assertEquals(0, vo.getProcessedCount());
        assertEquals(1, vo.getSkipped().size());
        verify(fixedAssetService, never()).createSurplusVoucher(any(), any(), any());
    }

    @Test
    void bookSurplus_rejectsNonPositiveAmount() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem it = surplusItem("item-1", "asset-1", 1, 2);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(it));
        when(itemMapper.selectById("item-1")).thenReturn(it);

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "0")));

        assertEquals(0, vo.getProcessedCount());
        assertEquals(1, vo.getSkipped().size());
        verify(fixedAssetService, never()).createSurplusVoucher(any(), any(), any());
    }

    @Test
    void bookSurplus_skipsDisposedAsset() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem it = surplusItem("item-1", "asset-1", 1, 2);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(it));
        when(itemMapper.selectById("item-1")).thenReturn(it);
        FixedAsset disposed = asset("asset-1", "FA-1", 1, "1000");
        disposed.setStatus(FixedAssetStatus.DISPOSED.name());
        when(assetMapper.selectById("asset-1")).thenReturn(disposed);

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "800")));

        assertEquals(0, vo.getProcessedCount());
        assertEquals(1, vo.getSkipped().size());
        verify(fixedAssetService, never()).createSurplusVoucher(any(), any(), any());
    }

    @Test
    void bookSurplus_splitsCardWhenBookQuantityIsOne() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem it = surplusItem("item-1", "asset-1", 1, 3);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(it));
        when(itemMapper.selectById("item-1")).thenReturn(it);
        FixedAsset src = asset("asset-1", "FA-1", 1, "1000");
        src.setAccumDepr(new BigDecimal("100"));
        src.setImpairment(new BigDecimal("50"));
        src.setTaxAmount(new BigDecimal("30"));
        src.setPurchaseVoucherId("pv-old");
        when(assetMapper.selectById("asset-1")).thenReturn(src);
        doAnswer(inv -> {
            FixedAsset a = inv.getArgument(0);
            a.setId("asset-new");
            return 1;
        }).when(assetMapper).insert(any(FixedAsset.class));
        when(fixedAssetService.createSurplusVoucher(any(FixedAsset.class), eq(new BigDecimal("800.00")), any()))
                .thenReturn("voucher-1");

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "800")));

        assertEquals(1, vo.getProcessedCount());
        ArgumentCaptor<FixedAsset> inserted = ArgumentCaptor.forClass(FixedAsset.class);
        verify(assetMapper).insert(inserted.capture());
        FixedAsset clone = inserted.getValue();
        assertEquals("FA-1-副本", clone.getCode());
        assertEquals(2, clone.getQuantity());
        assertEquals(new BigDecimal("800.00"), clone.getOriginalValue());
        assertEquals(0, clone.getAccumDepr().compareTo(BigDecimal.ZERO));
        assertEquals(0, clone.getImpairment().compareTo(BigDecimal.ZERO));
        assertEquals(0, clone.getTaxAmount().compareTo(BigDecimal.ZERO));
        assertEquals(null, clone.getPurchaseVoucherId());
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
        ArgumentCaptor<String> summary = ArgumentCaptor.forClass(String.class);
        verify(fixedAssetService).createSurplusVoucher(any(FixedAsset.class), eq(new BigDecimal("800.00")), summary.capture());
        assertEquals(true, summary.getValue().contains("盘盈") && summary.getValue().contains("盘点单"));
        assertEquals("voucher-1", it.getSurplusVoucherId());
        assertEquals("asset-new", it.getSurplusAssetId());
        assertEquals(new BigDecimal("800.00"), it.getSurplusAmount());
        verify(itemMapper).updateById(it);
    }

    @Test
    void bookSurplus_bumpsQuantityWhenBookQuantityAboveOne() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem it = surplusItem("item-1", "asset-2", 2, 3);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(it));
        when(itemMapper.selectById("item-1")).thenReturn(it);
        FixedAsset src = asset("asset-2", "FA-2", 2, "1000");
        when(assetMapper.selectById("asset-2")).thenReturn(src);
        when(fixedAssetService.createSurplusVoucher(any(FixedAsset.class), eq(new BigDecimal("500.00")), any()))
                .thenReturn("voucher-2");

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "500")));

        assertEquals(1, vo.getProcessedCount());
        verify(assetMapper, never()).insert(any(FixedAsset.class));
        assertEquals(3, src.getQuantity());
        assertEquals(0, src.getOriginalValue().compareTo(new BigDecimal("1500")));
        verify(assetMapper).updateById(src);
        assertEquals("voucher-2", it.getSurplusVoucherId());
        assertEquals(null, it.getSurplusAssetId());
    }

    @Test
    void bookSurplus_skipsBumpWhenCardHasDepreciation() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem it = surplusItem("item-1", "asset-2", 2, 3);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(it));
        when(itemMapper.selectById("item-1")).thenReturn(it);
        FixedAsset src = asset("asset-2", "FA-2", 2, "1000");
        src.setAccumDepr(new BigDecimal("200"));
        when(assetMapper.selectById("asset-2")).thenReturn(src);

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "500")));

        assertEquals(0, vo.getProcessedCount());
        assertTrue(vo.getSkipped().get(0).getReason().contains("禁止 bump"));
        verify(fixedAssetService, never()).createSurplusVoucher(any(), any(), any());
        verify(assetMapper, never()).updateById(any(FixedAsset.class));
    }

    @Test
    void bookSurplus_voucherFailureIsSkippedAndDoesNotTouchCards() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem it = surplusItem("item-1", "asset-1", 1, 2);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(it));
        when(itemMapper.selectById("item-1")).thenReturn(it);
        when(assetMapper.selectById("asset-1")).thenReturn(asset("asset-1", "FA-1", 1, "1000"));
        when(fixedAssetService.createSurplusVoucher(any(), any(), any()))
                .thenThrow(new BusinessException(400, "缺少科目"));

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "800")));

        assertEquals(0, vo.getProcessedCount());
        assertEquals("缺少科目", vo.getSkipped().get(0).getReason());
        verify(assetMapper, never()).insert(any(FixedAsset.class));
        verify(itemMapper, never()).updateById(any(FixedAssetCheckItem.class));
    }

    @Test
    void bookSurplus_failureAfterVoucherRollsBackTransactionAndIsSkipped() {
        when(checkMapper.selectById("check-1")).thenReturn(completedCheck());
        FixedAssetCheckItem it = surplusItem("item-1", "asset-2", 2, 3);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(it));
        when(itemMapper.selectById("item-1")).thenReturn(it);
        when(assetMapper.selectById("asset-2")).thenReturn(asset("asset-2", "FA-2", 2, "1000"));
        when(fixedAssetService.createSurplusVoucher(any(FixedAsset.class), any(), any())).thenReturn("voucher-x");
        doAnswer(inv -> {
            throw new RuntimeException();
        }).when(itemMapper).updateById(any(FixedAssetCheckItem.class));

        FixedAssetCheckDtos.SurplusBookVo vo = service.bookSurplus("check-1", BOOK_ID,
                List.of(bookDto("item-1", "500")));

        assertEquals(0, vo.getProcessedCount());
        assertEquals("入账失败", vo.getSkipped().get(0).getReason());
        ArgumentCaptor<TransactionDefinition> def = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(def.capture());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, def.getValue().getPropagationBehavior());
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    private FixedAssetCheck completedCheck() {
        FixedAssetCheck check = draftCheck();
        check.setStatus(FixedAssetCheck.STATUS_COMPLETED);
        return check;
    }

    private FixedAssetCheckItem surplusItem(String id, String assetId, int book, int actual) {
        FixedAssetCheckItem it = item(id, book);
        it.setAssetId(assetId);
        it.setAssetCode("C-" + assetId);
        it.setActualQuantity(actual);
        it.setResult(FixedAssetCheckItem.RESULT_SURPLUS);
        return it;
    }

    private FixedAsset asset(String id, String code, int qty, String originalValue) {
        FixedAsset a = new FixedAsset();
        a.setId(id);
        a.setBookId(BOOK_ID);
        a.setCode(code);
        a.setName("资产" + code);
        a.setQuantity(qty);
        a.setOriginalValue(new BigDecimal(originalValue));
        a.setStatus(FixedAssetStatus.IN_USE.name());
        return a;
    }

    private FixedAssetCheckDtos.SurplusBookItemDto bookDto(String itemId, String amount) {
        FixedAssetCheckDtos.SurplusBookItemDto dto = new FixedAssetCheckDtos.SurplusBookItemDto();
        dto.setItemId(itemId);
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }

    private FixedAssetCheck draftCheck() {
        FixedAssetCheck check = new FixedAssetCheck();
        check.setId("check-1");
        check.setBookId(BOOK_ID);
        check.setTitle("盘点单");
        check.setCheckDate(new Date());
        check.setStatus(FixedAssetCheck.STATUS_DRAFT);
        return check;
    }

    private FixedAssetCheckItem item(String id, int bookQuantity) {
        FixedAssetCheckItem item = new FixedAssetCheckItem();
        item.setId(id);
        item.setBookId(BOOK_ID);
        item.setCheckId("check-1");
        item.setAssetId("asset-x");
        item.setAssetCode("FA-X");
        item.setAssetName("资产X");
        item.setBookQuantity(bookQuantity);
        return item;
    }
}
