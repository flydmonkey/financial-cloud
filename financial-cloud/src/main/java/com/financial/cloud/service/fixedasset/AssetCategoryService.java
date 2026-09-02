package com.financial.cloud.service.fixedasset;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.fixedasset.AssetCategory;
import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.fixedasset.AssetCategoryChangeDto;
import com.financial.cloud.dto.fixedasset.AssetCategoryPageDto;
import com.financial.cloud.dto.fixedasset.AssetCategoryVo;
import com.financial.cloud.enums.error.FixedAssetErrorCode;
import com.financial.cloud.enums.fixedasset.DepreciationMethod;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.repository.fixedasset.AssetCategoryMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetMapper;
import com.financial.cloud.service.book.BookSubjectService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AssetCategoryService extends ServiceImpl<AssetCategoryMapper, AssetCategory> {

    private final AssetCategoryMapper assetCategoryMapper;
    private final FixedAssetMapper fixedAssetMapper;
    private final IdentifierGenerator identifierGenerator;
    private final BookSubjectService bookSubjectService;

    public Message<AssetCategoryVo> getById(String id) {
        AssetCategory entity = assetCategoryMapper.selectById(id);
        return Message.ok(entity == null ? null : toVo(entity));
    }

    public Message<Page<AssetCategoryVo>> pageList(AssetCategoryPageDto dto) {
        Page<AssetCategory> page = assetCategoryMapper.selectPage(dto.build(), buildQueryWrapper(dto));
        Page<AssetCategoryVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isNotEmpty(page.getRecords())) {
            Map<String, String> subjectNames = loadSubjectNames(dto.getBookId(), page.getRecords());
            result.setRecords(page.getRecords().stream()
                    .map(e -> toVo(e, subjectNames))
                    .collect(Collectors.toList()));
        }
        return Message.ok(result);
    }

    public Message<List<AssetCategoryVo>> listAll(String bookId) {
        List<AssetCategory> list = assetCategoryMapper.selectList(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getBookId, bookId)
                .orderByAsc(AssetCategory::getCode));
        Map<String, String> subjectNames = loadSubjectNames(bookId, list);
        return Message.ok(list.stream().map(e -> toVo(e, subjectNames)).collect(Collectors.toList()));
    }

    @Transactional
    public Message<String> save(AssetCategoryChangeDto dto) {
        normalizeLife(dto);
        applyDefaultSubjects(dto);
        AssetCategory entity = AssetCategory.builder().build();
        BeanUtil.copyProperties(dto, entity);
        validBeforeSave(entity);
        String id = identifierGenerator.nextId(entity).toString();
        entity.setId(id);
        boolean ok = super.save(entity);
        return ok ? new Message<>(Message.SUCCESS, "新增成功", id) : new Message<>(Message.FAIL, "新增失败");
    }

    @Transactional
    public Message<String> update(AssetCategoryChangeDto dto) {
        normalizeLife(dto);
        applyDefaultSubjects(dto);
        AssetCategory entity = AssetCategory.builder().build();
        BeanUtil.copyProperties(dto, entity);
        validBeforeSave(entity);
        boolean ok = super.updateById(entity);
        return ok ? new Message<>(Message.SUCCESS, "修改成功", dto.getId()) : new Message<>(Message.FAIL, "修改失败");
    }

    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> ids = dto.getListIds();
        if (CollUtil.isEmpty(ids)) {
            return new Message<>(Message.FAIL, "删除失败");
        }
        Long used = fixedAssetMapper.selectCount(Wrappers.<FixedAsset>lambdaQuery()
                .in(FixedAsset::getCategoryId, ids));
        if (used != null && used > 0) {
            throw new ServiceException(FixedAssetErrorCode.CATEGORY_IN_USE);
        }
        boolean ok = super.removeBatchByIds(ids);
        return ok ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }

    private void normalizeLife(AssetCategoryChangeDto dto) {
        DepreciationMethod method = DepreciationMethod.from(dto.getDepreciationMethod());
        if (method == DepreciationMethod.NONE) {
            if (dto.getUsefulLifeMonths() == null) {
                dto.setUsefulLifeMonths(0);
            }
            if (dto.getUsefulLifeYears() == null) {
                dto.setUsefulLifeYears(0);
            }
            return;
        }
        if (dto.getUsefulLifeMonths() == null && dto.getUsefulLifeYears() != null) {
            dto.setUsefulLifeMonths(dto.getUsefulLifeYears() * 12);
        }
        if (dto.getUsefulLifeYears() == null && dto.getUsefulLifeMonths() != null) {
            dto.setUsefulLifeYears(dto.getUsefulLifeMonths() / 12);
        }
    }

    private void applyDefaultSubjects(AssetCategoryChangeDto dto) {
        if (StringUtils.isBlank(dto.getFixedAssetSubjectId())) {
            BookSubject fa = bookSubjectService.selectSubject(dto.getBookId(), "1601");
            if (fa != null) {
                dto.setFixedAssetSubjectId(fa.getId());
            }
        }
        if (StringUtils.isBlank(dto.getAccumDeprSubjectId())) {
            BookSubject accum = bookSubjectService.selectSubject(dto.getBookId(), "1602");
            if (accum != null) {
                dto.setAccumDeprSubjectId(accum.getId());
            }
        }
    }

    private void validBeforeSave(AssetCategory entity) {
        AssetCategory exists = assetCategoryMapper.selectOne(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getBookId, entity.getBookId())
                .eq(AssetCategory::getCode, entity.getCode())
                .ne(StringUtils.isNotBlank(entity.getId()), AssetCategory::getId, entity.getId()));
        if (exists != null) {
            throw new ServiceException(FixedAssetErrorCode.CATEGORY_CODE_DUPLICATE);
        }
    }

    private LambdaQueryWrapper<AssetCategory> buildQueryWrapper(AssetCategoryPageDto dto) {
        return Wrappers.<AssetCategory>lambdaQuery()
                .eq(StringUtils.isNotBlank(dto.getBookId()), AssetCategory::getBookId, dto.getBookId())
                .eq(StringUtils.isNotBlank(dto.getCode()), AssetCategory::getCode, dto.getCode())
                .like(StringUtils.isNotBlank(dto.getName()), AssetCategory::getName, dto.getName())
                .ne(StringUtils.isNotBlank(dto.getNoId()), AssetCategory::getId, dto.getNoId())
                .orderByAsc(AssetCategory::getCode);
    }

    private AssetCategoryVo toVo(AssetCategory entity) {
        return toVo(entity, loadSubjectNames(entity.getBookId(), List.of(entity)));
    }

    private AssetCategoryVo toVo(AssetCategory entity, Map<String, String> subjectNames) {
        AssetCategoryVo vo = BeanUtil.copyProperties(entity, AssetCategoryVo.class);
        vo.setFixedAssetSubjectName(subjectNames.get(entity.getFixedAssetSubjectId()));
        vo.setAccumDeprSubjectName(subjectNames.get(entity.getAccumDeprSubjectId()));
        try {
            vo.setDepreciationMethodLabel(DepreciationMethod.from(entity.getDepreciationMethod()).getLabel());
        } catch (Exception ignored) {
            vo.setDepreciationMethodLabel(entity.getDepreciationMethod());
        }
        return vo;
    }

    private Map<String, String> loadSubjectNames(String bookId, List<AssetCategory> records) {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isBlank(bookId) || CollUtil.isEmpty(records)) {
            return map;
        }
        List<String> ids = records.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getFixedAssetSubjectId(), r.getAccumDeprSubjectId()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return map;
        }
        List<BookSubject> subjects = bookSubjectService.listByIds(ids);
        for (BookSubject s : subjects) {
            map.put(s.getId(), s.getCode() + " " + s.getName());
        }
        return map;
    }
}
