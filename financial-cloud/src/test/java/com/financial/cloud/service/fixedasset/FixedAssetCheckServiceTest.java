package com.financial.cloud.service.fixedasset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
