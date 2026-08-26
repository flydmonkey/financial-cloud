package com.financial.cloud.service.book;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.AssistAcc;
import com.financial.cloud.dto.book.AssistAccVo;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.book.AssistAccChangeDto;
import com.financial.cloud.dto.book.AssistAccPageDto;
import com.financial.cloud.enums.AssistErrorCode;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.repository.book.AssistAccMapper;
import com.financial.cloud.service.book.AssistAccService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 辅助核算项目Service业务层处理
 *
 * @author wuyan
 * {@code @date} 2025-02-18
 */
@RequiredArgsConstructor
@Service
public class AssistAccService extends ServiceImpl<AssistAccMapper, AssistAcc>{

    private final AssistAccMapper assistAccMapper;
    private final IdentifierGenerator identifierGenerator;
    public Message<AssistAccVo> getById(String id) {
        AssistAcc assistAcc = assistAccMapper.selectById(id);
        return Message.ok(assistAcc == null ? null : BeanUtil.copyProperties(assistAcc, AssistAccVo.class));
    }

    /**
     * 分页查询
     *
     * @param dto 分页参数
     * @return 查询结果
     */
    public Message<Page<AssistAccVo>> pageList(AssistAccPageDto dto) {
        LambdaQueryWrapper<AssistAcc> lqw = buildQueryWrapper(dto);
        Page<AssistAcc> page = assistAccMapper.selectPage(dto.build(), lqw);
        Page<AssistAccVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isNotEmpty(page.getRecords())) {
            result.setRecords(BeanUtil.copyToList(page.getRecords(), AssistAccVo.class));
        }
        return Message.ok(result);
    }

    /**
     * 插入数据
     *
     * @param dto 插入对象
     * @return 插入结果
     */
    @Transactional
    public Message<String> save(AssistAccChangeDto dto) {
        AssistAcc assistAcc = AssistAcc.builder().build();
        BeanUtil.copyProperties(dto, assistAcc);
        validEntityBeforeSave(assistAcc);
        String currentId = identifierGenerator.nextId(assistAcc).toString();
        assistAcc.setId(currentId);
        boolean save = super.save(assistAcc);
        return save ? new Message<>(Message.SUCCESS, "新增成功", currentId) : new Message<>(Message.FAIL, "新增失败");
    }

    /**
     * 更新信息
     *
     * @param dto 更新对象
     * @return 结果
     */
    @Transactional
    public Message<String> update(AssistAccChangeDto dto) {
        AssistAcc assistAcc = AssistAcc.builder().build();
        BeanUtil.copyProperties(dto, assistAcc);
        validEntityBeforeSave(assistAcc);
        String currentId = dto.getId();
        boolean update = super.updateById(assistAcc);
        return update ? new Message<>(Message.SUCCESS, "修改成功", currentId) : new Message<>(Message.FAIL, "修改失败");
    }


    /**
     * 根据ID删除
     *
     * @param dto ID组
     * @return 结果
     */
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> ids = dto.getListIds();
        boolean result = super.removeBatchByIds(ids);
        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }

    /**
     * 构建查询条件
     *
     * @param bo 查询参数
     */
    private LambdaQueryWrapper<AssistAcc> buildQueryWrapper(AssistAccPageDto bo) {
        LambdaQueryWrapper<AssistAcc> lqw = Wrappers.lambdaQuery();
        lqw.ne(StringUtils.isNotBlank(bo.getNoId()), AssistAcc::getId, bo.getNoId());
        lqw.eq(StringUtils.isNotBlank(bo.getBookId()), AssistAcc::getBookId, bo.getBookId());
        lqw.eq(StringUtils.isNotBlank(bo.getAssistType()), AssistAcc::getAssistType, bo.getAssistType());
        lqw.eq(StringUtils.isNotBlank(bo.getAssistCode()), AssistAcc::getAssistCode, bo.getAssistCode());
        lqw.likeLeft(StringUtils.isNotBlank(bo.getAssistName()), AssistAcc::getAssistName, bo.getAssistName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), AssistAcc::getStatus, bo.getStatus());
        return lqw;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(AssistAcc entity) {
        AssistAccPageDto dto = AssistAccPageDto.builder()
                .noId(entity.getId())
                .bookId(entity.getBookId())
                .assistCode(entity.getAssistCode())
                .assistType(entity.getAssistType())
                .build();
        LambdaQueryWrapper<AssistAcc> lqw = buildQueryWrapper(dto);
        AssistAcc assistAcc = assistAccMapper.selectOne(lqw);
        if (assistAcc != null) {
            throw new ServiceException(AssistErrorCode.CODE_DUPLICATE);
        }
    }
}
