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

import java.util.Date;
import java.util.List;

/**
 * 资产盘点：建单快照在册资产 → 实盘录入 → 完成盘点自动判定盘盈/盘亏/正常。
 * 只记录结果与出盘点表；盘盈盘亏的账务处理（凭证）为后续增强。
 */
@Service
@RequiredArgsConstructor
public class FixedAssetCheckService {

    private final FixedAssetCheckMapper checkMapper;
    private final FixedAssetCheckItemMapper itemMapper;
    private final FixedAssetMapper assetMapper;
    private final BookSealGuard bookSealGuard;

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
