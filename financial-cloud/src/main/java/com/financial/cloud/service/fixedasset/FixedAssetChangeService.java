package com.financial.cloud.service.fixedasset;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.domain.fixedasset.FixedAssetChange;
import com.financial.cloud.domain.fixedasset.FixedAssetChangeItem;
import com.financial.cloud.dto.fixedasset.FixedAssetChangeItemDto;
import com.financial.cloud.dto.fixedasset.FixedAssetChangePageDto;
import com.financial.cloud.dto.fixedasset.FixedAssetChangeSaveDto;
import com.financial.cloud.dto.fixedasset.FixedAssetChangeVo;
import com.financial.cloud.enums.error.FixedAssetErrorCode;
import com.financial.cloud.enums.fixedasset.DepreciationMethod;
import com.financial.cloud.enums.fixedasset.FixedAssetChangeField;
import com.financial.cloud.enums.fixedasset.FixedAssetStatus;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.repository.fixedasset.FixedAssetChangeItemMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetChangeMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.util.FixedAssetDepreciationRules;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FixedAssetChangeService {

    private final FixedAssetChangeMapper changeMapper;
    private final FixedAssetChangeItemMapper changeItemMapper;
    private final FixedAssetMapper fixedAssetMapper;
    private final IdentifierGenerator identifierGenerator;
    private final ConfigSysService configSysService;

    public Message<Page<FixedAssetChangeVo>> pageList(FixedAssetChangePageDto dto) {
        String start = dto.getStartPeriod();
        String end = dto.getEndPeriod();
        if (StringUtils.isBlank(start) || StringUtils.isBlank(end)) {
            String current = configSysService.getCurrentTerm(dto.getBookId());
            start = StringUtils.defaultIfBlank(start, current);
            end = StringUtils.defaultIfBlank(end, current);
        }
        if (start.compareTo(end) > 0) {
            String tmp = start;
            start = end;
            end = tmp;
        }

        List<FixedAssetChange> changes = changeMapper.selectList(Wrappers.<FixedAssetChange>lambdaQuery()
                .eq(FixedAssetChange::getBookId, dto.getBookId())
                .ge(FixedAssetChange::getYearPeriod, start)
                .le(FixedAssetChange::getYearPeriod, end)
                .orderByDesc(FixedAssetChange::getCreatedDate));
        if (changes.isEmpty()) {
            Page<FixedAssetChangeVo> empty = new Page<>(dto.getPageNumber(), dto.getPageSize(), 0);
            empty.setRecords(List.of());
            return Message.ok(empty);
        }
        Map<String, FixedAssetChange> changeMap = changes.stream()
                .collect(Collectors.toMap(FixedAssetChange::getId, c -> c, (a, b) -> a));
        List<String> changeIds = changes.stream().map(FixedAssetChange::getId).toList();

        List<String> assetIdsFilter = null;
        if (StringUtils.isNotBlank(dto.getAssetCode()) || StringUtils.isNotBlank(dto.getAssetName())) {
            List<FixedAsset> assets = fixedAssetMapper.selectList(Wrappers.<FixedAsset>lambdaQuery()
                    .eq(FixedAsset::getBookId, dto.getBookId())
                    .like(StringUtils.isNotBlank(dto.getAssetCode()), FixedAsset::getCode, dto.getAssetCode())
                    .like(StringUtils.isNotBlank(dto.getAssetName()), FixedAsset::getName, dto.getAssetName()));
            assetIdsFilter = assets.stream().map(FixedAsset::getId).collect(Collectors.toList());
            if (assetIdsFilter.isEmpty()) {
                Page<FixedAssetChangeVo> empty = new Page<>(dto.getPageNumber(), dto.getPageSize(), 0);
                empty.setRecords(List.of());
                return Message.ok(empty);
            }
        }

        Page<FixedAssetChangeItem> page = changeItemMapper.selectPage(dto.build(),
                Wrappers.<FixedAssetChangeItem>lambdaQuery()
                        .eq(FixedAssetChangeItem::getBookId, dto.getBookId())
                        .in(FixedAssetChangeItem::getChangeId, changeIds)
                        .in(assetIdsFilter != null, FixedAssetChangeItem::getAssetId, assetIdsFilter)
                        .orderByDesc(FixedAssetChangeItem::getCreatedDate)
                        .orderByDesc(FixedAssetChangeItem::getId));

        Map<String, FixedAsset> assetMap = loadAssets(page.getRecords());
        List<FixedAssetChangeVo> records = new ArrayList<>();
        for (FixedAssetChangeItem item : page.getRecords()) {
            FixedAssetChange change = changeMap.get(item.getChangeId());
            FixedAsset asset = assetMap.get(item.getAssetId());
            FixedAssetChangeVo vo = new FixedAssetChangeVo();
            vo.setId(item.getId());
            vo.setChangeId(item.getChangeId());
            vo.setAssetId(item.getAssetId());
            vo.setAssetCode(asset != null ? asset.getCode() : null);
            vo.setAssetName(asset != null ? asset.getName() : null);
            vo.setFieldCode(item.getFieldCode());
            vo.setFieldLabel(item.getFieldLabel());
            vo.setBeforeValue(formatDisplay(item.getFieldCode(), item.getBeforeValue()));
            vo.setAfterValue(formatDisplay(item.getFieldCode(), item.getAfterValue()));
            vo.setYearPeriod(change != null ? change.getYearPeriod() : null);
            vo.setModifiedBy(change != null ? change.getCreatedBy() : item.getCreatedBy());
            vo.setModifiedByName(vo.getModifiedBy());
            vo.setChangeTime(change != null ? change.getCreatedDate() : item.getCreatedDate());
            records.add(vo);
        }
        Page<FixedAssetChangeVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return Message.ok(result);
    }

    @Transactional
    public Message<String> saveChange(FixedAssetChangeSaveDto dto) {
        if (dto == null || CollUtil.isEmpty(dto.getItems())) {
            throw new ServiceException(FixedAssetErrorCode.CHANGE_ITEMS_EMPTY);
        }
        FixedAsset asset = fixedAssetMapper.selectById(dto.getAssetId());
        if (asset == null || !Objects.equals(asset.getBookId(), dto.getBookId())) {
            throw new ServiceException(FixedAssetErrorCode.ASSET_NOT_FOUND);
        }
        String period = StringUtils.defaultIfBlank(dto.getYearPeriod(), configSysService.getCurrentTerm(dto.getBookId()));

        List<FixedAssetChangeItem> items = new ArrayList<>();
        for (FixedAssetChangeItemDto itemDto : dto.getItems()) {
            if (itemDto == null || StringUtils.isBlank(itemDto.getFieldCode())) {
                continue;
            }
            FixedAssetChangeField field = FixedAssetChangeField.fromCode(itemDto.getFieldCode());
            String before = readField(asset, field);
            String after = normalizeValue(field, itemDto.getAfterValue());
            if (Objects.equals(StringUtils.defaultString(before), StringUtils.defaultString(after))) {
                continue;
            }
            FixedAssetChangeItem item = FixedAssetChangeItem.builder()
                    .bookId(dto.getBookId())
                    .assetId(asset.getId())
                    .fieldCode(field.getCode())
                    .fieldLabel(field.getLabel())
                    .beforeValue(before)
                    .afterValue(after)
                    .build();
            items.add(item);
            applyField(asset, field, after);
        }
        if (items.isEmpty()) {
            throw new ServiceException(FixedAssetErrorCode.CHANGE_NO_DIFF);
        }

        DepreciationMethod method = DepreciationMethod.from(asset.getDepreciationMethod());
        if (method.isAccelerated()
                && !FixedAssetDepreciationRules.isValidAcceleratedLife(asset.getUsefulLifeMonths())) {
            throw new ServiceException(FixedAssetErrorCode.ACCELERATED_LIFE_INVALID);
        }
        fixedAssetMapper.updateById(asset);

        FixedAssetChange change = FixedAssetChange.builder()
                .bookId(dto.getBookId())
                .assetId(asset.getId())
                .yearPeriod(period)
                .remark(dto.getRemark())
                .build();
        change.setId(identifierGenerator.nextId(change).toString());
        changeMapper.insert(change);
        for (FixedAssetChangeItem item : items) {
            item.setChangeId(change.getId());
            item.setId(identifierGenerator.nextId(item).toString());
            changeItemMapper.insert(item);
        }
        return new Message<>(Message.SUCCESS, "变动保存成功", change.getId());
    }

    /**
     * 卡片普通保存 / 清理时记录非计算（或状态）变动流水。
     */
    @Transactional
    public void recordAutoChange(String bookId, FixedAsset before, FixedAsset after, String remark) {
        if (before == null || after == null) {
            return;
        }
        List<FixedAssetChangeItemDto> diffs = new ArrayList<>();
        for (FixedAssetChangeField field : FixedAssetChangeField.values()) {
            if (field.isCalcField()) {
                continue;
            }
            String b = readField(before, field);
            String a = readField(after, field);
            if (!Objects.equals(StringUtils.defaultString(b), StringUtils.defaultString(a))) {
                FixedAssetChangeItemDto item = new FixedAssetChangeItemDto();
                item.setFieldCode(field.getCode());
                item.setAfterValue(a);
                diffs.add(item);
            }
        }
        if (diffs.isEmpty()) {
            return;
        }
        FixedAssetChangeSaveDto dto = new FixedAssetChangeSaveDto();
        dto.setBookId(bookId);
        dto.setAssetId(after.getId());
        dto.setRemark(remark);
        dto.setItems(diffs);
        // 避免再次 apply：直接写流水（资产已更新）
        writeLogOnly(dto, before);
    }

    private void writeLogOnly(FixedAssetChangeSaveDto dto, FixedAsset beforeSnapshot) {
        String period = StringUtils.defaultIfBlank(dto.getYearPeriod(), configSysService.getCurrentTerm(dto.getBookId()));
        FixedAssetChange change = FixedAssetChange.builder()
                .bookId(dto.getBookId())
                .assetId(dto.getAssetId())
                .yearPeriod(period)
                .remark(dto.getRemark())
                .build();
        change.setId(identifierGenerator.nextId(change).toString());
        changeMapper.insert(change);
        for (FixedAssetChangeItemDto itemDto : dto.getItems()) {
            FixedAssetChangeField field = FixedAssetChangeField.fromCode(itemDto.getFieldCode());
            FixedAssetChangeItem item = FixedAssetChangeItem.builder()
                    .bookId(dto.getBookId())
                    .changeId(change.getId())
                    .assetId(dto.getAssetId())
                    .fieldCode(field.getCode())
                    .fieldLabel(field.getLabel())
                    .beforeValue(readField(beforeSnapshot, field))
                    .afterValue(itemDto.getAfterValue())
                    .build();
            item.setId(identifierGenerator.nextId(item).toString());
            changeItemMapper.insert(item);
        }
    }

    private Map<String, FixedAsset> loadAssets(List<FixedAssetChangeItem> items) {
        Map<String, FixedAsset> map = new HashMap<>();
        if (CollUtil.isEmpty(items)) {
            return map;
        }
        List<String> ids = items.stream().map(FixedAssetChangeItem::getAssetId).distinct().toList();
        for (FixedAsset a : fixedAssetMapper.selectBatchIds(ids)) {
            map.put(a.getId(), a);
        }
        return map;
    }

    private String readField(FixedAsset asset, FixedAssetChangeField field) {
        return switch (field) {
            case NAME -> asset.getName();
            case DEPT_ID -> asset.getDeptId();
            case LOCATION -> asset.getLocation();
            case SPEC -> asset.getSpec();
            case QUANTITY -> asset.getQuantity() == null ? null : String.valueOf(asset.getQuantity());
            case USER_ID -> asset.getUserId();
            case STATUS -> asset.getStatus();
            case ORIGINAL_VALUE -> toPlain(asset.getOriginalValue());
            case RESIDUAL_RATE -> toPlain(asset.getResidualRate());
            case USEFUL_LIFE_MONTHS -> asset.getUsefulLifeMonths() == null ? null : String.valueOf(asset.getUsefulLifeMonths());
            case EXPECTED_TOTAL_WORK -> toPlain(asset.getExpectedTotalWork());
            case DEPRECIATION_METHOD -> asset.getDepreciationMethod();
            case IMPAIRMENT -> toPlain(asset.getImpairment());
        };
    }

    private void applyField(FixedAsset asset, FixedAssetChangeField field, String value) {
        switch (field) {
            case NAME -> asset.setName(value);
            case DEPT_ID -> asset.setDeptId(value);
            case LOCATION -> asset.setLocation(value);
            case SPEC -> asset.setSpec(value);
            case QUANTITY -> asset.setQuantity(StringUtils.isBlank(value) ? null : Integer.valueOf(value));
            case USER_ID -> asset.setUserId(value);
            case STATUS -> asset.setStatus(value);
            case ORIGINAL_VALUE -> asset.setOriginalValue(parseDecimal(value));
            case RESIDUAL_RATE -> asset.setResidualRate(parseDecimal(value));
            case USEFUL_LIFE_MONTHS -> asset.setUsefulLifeMonths(StringUtils.isBlank(value) ? null : Integer.valueOf(value));
            case EXPECTED_TOTAL_WORK -> asset.setExpectedTotalWork(parseDecimal(value));
            case DEPRECIATION_METHOD -> asset.setDepreciationMethod(value);
            case IMPAIRMENT -> asset.setImpairment(parseDecimal(value));
        }
    }

    private String normalizeValue(FixedAssetChangeField field, String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (field == FixedAssetChangeField.DEPRECIATION_METHOD) {
            return DepreciationMethod.from(value).name();
        }
        if (field == FixedAssetChangeField.STATUS) {
            return StringUtils.upperCase(value);
        }
        return value;
    }

    private String formatDisplay(String fieldCode, String raw) {
        if (raw == null) {
            return "";
        }
        if (FixedAssetChangeField.DEPRECIATION_METHOD.getCode().equals(fieldCode)) {
            try {
                return DepreciationMethod.from(raw).getLabel();
            } catch (Exception e) {
                return raw;
            }
        }
        if (FixedAssetChangeField.STATUS.getCode().equals(fieldCode)) {
            return FixedAssetStatus.from(raw).getLabel();
        }
        return raw;
    }

    private String toPlain(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    private BigDecimal parseDecimal(String v) {
        if (StringUtils.isBlank(v)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(v);
    }
}
