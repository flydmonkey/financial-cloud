package com.financial.cloud.service.fixedasset;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 资产盘点：建单快照在册资产 → 实盘录入 → 完成盘点自动判定盘盈/盘亏/正常。
 * 盘亏可一键下账（整件盘亏复用资产清理流程生成凭证并下账；部分盘亏需先做资产拆分）。
 * 盘盈因无卡片与计价依据，仅提示，不自动入账。
 */
@Service
@RequiredArgsConstructor
public class FixedAssetCheckService {

    private final FixedAssetCheckMapper checkMapper;
    private final FixedAssetCheckItemMapper itemMapper;
    private final FixedAssetMapper assetMapper;
    private final BookSealGuard bookSealGuard;
    private final FixedAssetService fixedAssetService;

    public Page<FixedAssetCheck> page(FixedAssetCheckDtos.PageDto dto) {
        return checkMapper.selectPage(dto.build(), Wrappers.<FixedAssetCheck>lambdaQuery()
                .eq(FixedAssetCheck::getBookId, dto.getBookId())
                .eq(StringUtils.isNotBlank(dto.getStatus()), FixedAssetCheck::getStatus, dto.getStatus())
                .orderByDesc(FixedAssetCheck::getCheckDate)
                .orderByDesc(FixedAssetCheck::getCreatedDate));
    }

    public FixedAssetCheckDtos.DetailVo detail(String checkId, String bookId) {
        FixedAssetCheck check = requireCheck(checkId, bookId);
        List<FixedAssetCheckItem> items = itemMapper.selectList(Wrappers.<FixedAssetCheckItem>lambdaQuery()
                .eq(FixedAssetCheckItem::getCheckId, checkId)
                .eq(FixedAssetCheckItem::getBookId, bookId)
                .orderByAsc(FixedAssetCheckItem::getAssetCode));
        FixedAssetCheckDtos.DetailVo vo = new FixedAssetCheckDtos.DetailVo();
        vo.setCheck(check);
        vo.setItems(items);
        return vo;
    }

    /**
     * 新建盘点单：把当前在册（未清理）资产快照进明细行。
     */
    @Transactional
    public FixedAssetCheck create(FixedAssetCheckDtos.CreateDto dto, String bookId) {
        bookSealGuard.assertWritable(bookId);
        if (StringUtils.isBlank(dto.getTitle())) {
            throw new BusinessException(400, "盘点单标题不能为空");
        }
        Date checkDate = dto.getCheckDate() != null ? dto.getCheckDate() : new Date();

        List<FixedAsset> assets = assetMapper.selectList(Wrappers.<FixedAsset>lambdaQuery()
                .eq(FixedAsset::getBookId, bookId)
                .and(w -> w.ne(FixedAsset::getStatus, FixedAssetStatus.DISPOSED.name())
                        .or().isNull(FixedAsset::getStatus)));

        FixedAssetCheck check = new FixedAssetCheck();
        check.setBookId(bookId);
        check.setTitle(dto.getTitle());
        check.setCheckDate(checkDate);
        check.setStatus(FixedAssetCheck.STATUS_DRAFT);
        check.setRemark(dto.getRemark());
        check.setTotalCount(assets.size());
        check.setNormalCount(0);
        check.setSurplusCount(0);
        check.setDeficitCount(0);
        checkMapper.insert(check);

        List<FixedAssetCheckItem> items = assets.stream().map(asset -> FixedAssetCheckItem.builder()
                .bookId(bookId)
                .checkId(check.getId())
                .assetId(asset.getId())
                .assetCode(asset.getCode())
                .assetName(asset.getName())
                .location(asset.getLocation())
                .bookQuantity(asset.getQuantity() != null ? asset.getQuantity() : 1)
                .build()).toList();
        for (FixedAssetCheckItem item : items) {
            itemMapper.insert(item);
        }
        return check;
    }

    /**
     * 实盘录入：仅盘点中可改；结果在「完成盘点」时统一判定。
     */
    @Transactional
    public FixedAssetCheckItem updateItem(FixedAssetCheckDtos.ItemDto dto, String bookId) {
        bookSealGuard.assertWritable(bookId);
        FixedAssetCheckItem item = itemMapper.selectById(dto.getId());
        if (item == null || !bookId.equals(item.getBookId())) {
            throw new BusinessException(404, "盘点明细不存在或不属于当前账套");
        }
        FixedAssetCheck check = requireCheck(item.getCheckId(), bookId);
        requireDraft(check);

        if (dto.getActualQuantity() != null && dto.getActualQuantity() < 0) {
            throw new BusinessException(400, "实盘数量不能为负数");
        }
        item.setActualQuantity(dto.getActualQuantity());
        item.setActualLocation(dto.getActualLocation());
        item.setRemark(dto.getRemark());
        // 实时给出判定，便于界面展示；完成时会再统一复核
        if (dto.getActualQuantity() != null) {
            item.setResult(resolveResult(item.getBookQuantity(), dto.getActualQuantity()));
        }
        itemMapper.updateById(item);
        return item;
    }

    /**
     * 完成盘点：要求所有明细已录入实盘数，自动判定结果并汇总统计。
     */
    @Transactional
    public FixedAssetCheck complete(String checkId, String bookId) {
        bookSealGuard.assertWritable(bookId);
        FixedAssetCheck check = requireCheck(checkId, bookId);
        requireDraft(check);

        List<FixedAssetCheckItem> items = itemMapper.selectList(Wrappers.<FixedAssetCheckItem>lambdaQuery()
                .eq(FixedAssetCheckItem::getCheckId, checkId)
                .eq(FixedAssetCheckItem::getBookId, bookId));
        long unrecorded = items.stream().filter(i -> i.getActualQuantity() == null).count();
        if (unrecorded > 0) {
            throw new BusinessException(400, "还有 " + unrecorded + " 项资产未录入实盘数量");
        }

        int normal = 0;
        int surplus = 0;
        int deficit = 0;
        for (FixedAssetCheckItem item : items) {
            String result = resolveResult(item.getBookQuantity(), item.getActualQuantity());
            item.setResult(result);
            itemMapper.updateById(item);
            switch (result) {
                case FixedAssetCheckItem.RESULT_NORMAL -> normal++;
                case FixedAssetCheckItem.RESULT_SURPLUS -> surplus++;
                default -> deficit++;
            }
        }
        check.setStatus(FixedAssetCheck.STATUS_COMPLETED);
        check.setNormalCount(normal);
        check.setSurplusCount(surplus);
        check.setDeficitCount(deficit);
        check.setTotalCount(items.size());
        checkMapper.updateById(check);
        return check;
    }

    /**
     * 删除盘点单（仅盘点中）。
     */
    @Transactional
    public void delete(String checkId, String bookId) {
        bookSealGuard.assertWritable(bookId);
        FixedAssetCheck check = requireCheck(checkId, bookId);
        requireDraft(check);
        itemMapper.delete(Wrappers.<FixedAssetCheckItem>lambdaQuery()
                .eq(FixedAssetCheckItem::getCheckId, checkId));
        checkMapper.deleteById(checkId);
    }

    /**
     * 盘亏一键下账：对已完成盘点单中「整件盘亏」（实盘数=0）的资产，复用资产清理流程
     * 生成清理凭证并下账；部分盘亏跳过并提示先做资产拆分；盘盈仅计数提示。
     * 可重复调用：已下账的资产会被清理流程拒绝并记入跳过原因。
     * 不加类级事务：每项资产清理各自成事务，单项失败不影响其他项。
     */
    public FixedAssetCheckDtos.DeficitDisposeVo disposeDeficit(String checkId, String bookId) {
        bookSealGuard.assertWritable(bookId);
        FixedAssetCheck check = requireCheck(checkId, bookId);
        if (!FixedAssetCheck.STATUS_COMPLETED.equals(check.getStatus())) {
            throw new BusinessException(400, "盘点单未完成，不能执行盘亏下账");
        }

        List<FixedAssetCheckItem> items = itemMapper.selectList(Wrappers.<FixedAssetCheckItem>lambdaQuery()
                .eq(FixedAssetCheckItem::getCheckId, checkId)
                .eq(FixedAssetCheckItem::getBookId, bookId));

        FixedAssetCheckDtos.DeficitDisposeVo vo = new FixedAssetCheckDtos.DeficitDisposeVo();
        for (FixedAssetCheckItem item : items) {
            if (FixedAssetCheckItem.RESULT_SURPLUS.equals(item.getResult())) {
                vo.setSurplusCount(vo.getSurplusCount() + 1);
                continue;
            }
            if (!FixedAssetCheckItem.RESULT_DEFICIT.equals(item.getResult())) {
                continue;
            }
            if (item.getActualQuantity() != null && item.getActualQuantity() > 0) {
                vo.getSkipped().add(new FixedAssetCheckDtos.SkipReason(
                        item.getAssetCode(), item.getAssetName(), "部分盘亏，请先对资产卡片做拆分/变动后再下账"));
                continue;
            }
            try {
                com.financial.cloud.dto.fixedasset.FixedAssetDisposeDto disposeDto =
                        new com.financial.cloud.dto.fixedasset.FixedAssetDisposeDto();
                disposeDto.setSummary("盘亏下账（盘点单：" + check.getTitle() + "）："
                        + item.getAssetCode() + " " + item.getAssetName());
                com.financial.cloud.common.Message<?> result =
                        fixedAssetService.dispose(item.getAssetId(), bookId, disposeDto);
                if (result != null && result.getCode() == com.financial.cloud.common.Message.SUCCESS) {
                    vo.setProcessedCount(vo.getProcessedCount() + 1);
                } else {
                    vo.getSkipped().add(new FixedAssetCheckDtos.SkipReason(
                            item.getAssetCode(), item.getAssetName(),
                            result != null ? result.getMessage() : "清理失败"));
                }
            } catch (Exception e) {
                vo.getSkipped().add(new FixedAssetCheckDtos.SkipReason(
                        item.getAssetCode(), item.getAssetName(), e.getMessage()));
            }
        }
        return vo;
    }

    /** 结果判定：实盘 > 账面 盘盈；< 盘亏；= 正常 */
    static String resolveResult(Integer bookQuantity, Integer actualQuantity) {
        int book = bookQuantity != null ? bookQuantity : 0;
        int actual = actualQuantity != null ? actualQuantity : 0;
        if (actual > book) {
            return FixedAssetCheckItem.RESULT_SURPLUS;
        }
        if (actual < book) {
            return FixedAssetCheckItem.RESULT_DEFICIT;
        }
        return FixedAssetCheckItem.RESULT_NORMAL;
    }

    /** 盘盈默认入账金额：按原值在账面数量上均摊至盘盈数量 */
    static BigDecimal defaultSurplusAmount(BigDecimal originalValue, Integer bookQty, Integer actualQty) {
        int book = bookQty != null ? bookQty : 0;
        int actual = actualQty != null ? actualQty : 0;
        int delta = actual - book;
        if (delta <= 0 || book <= 0 || originalValue == null
                || originalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return originalValue
                .divide(BigDecimal.valueOf(book), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(delta))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private FixedAssetCheck requireCheck(String checkId, String bookId) {
        FixedAssetCheck check = checkMapper.selectById(checkId);
        if (check == null || !bookId.equals(check.getBookId())) {
            throw new BusinessException(404, "盘点单不存在或不属于当前账套");
        }
        return check;
    }

    private void requireDraft(FixedAssetCheck check) {
        if (!FixedAssetCheck.STATUS_DRAFT.equals(check.getStatus())) {
            throw new BusinessException(400, "盘点单已完成，不允许再修改");
        }
    }
}
