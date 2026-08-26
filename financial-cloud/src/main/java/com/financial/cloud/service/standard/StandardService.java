package com.financial.cloud.service.standard;


import lombok.RequiredArgsConstructor;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.standard.Standard;
import com.financial.cloud.domain.standard.StandardSubject;
import com.financial.cloud.dto.standard.StandardChangeDto;
import com.financial.cloud.dto.standard.StandardPageDto;
import com.financial.cloud.enums.BookBusinessExceptionEnum;
import com.financial.cloud.enums.StandardErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.standard.StandardMapper;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.standard.StandardSubjectMapper;
import com.financial.cloud.service.standard.StandardService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/27 16:43
 */

@RequiredArgsConstructor
@Slf4j
@Service
public class StandardService extends ServiceImpl<StandardMapper, Standard>{

    private final BookMapper bookMapper;

    private final StandardSubjectMapper standardSubjectMapper;
    public Message<Page<Standard>> pageList(StandardPageDto dto) {
        LambdaQueryWrapper<Standard> wrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(dto.getName())) {
            wrapper.like(Standard::getName, dto.getName());
        }

        Page<Standard> page = super.page(dto.build(), wrapper);

        return new Message<>(Message.SUCCESS, page);
    }
    public Message<String> save(StandardChangeDto dto) {

        Standard standard = new Standard();
        BeanUtil.copyProperties(dto, standard);

        boolean result = super.save(standard);

        return result ? new Message<>(Message.SUCCESS, "新增成功") : new Message<>(Message.FAIL, "新增失败");
    }
    public Message<String> update(StandardChangeDto dto) {
        if (dto.getStatus() == 0) {
            //禁用检查
            LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Book::getStandardId, dto.getId());
            List<Book> books = bookMapper.selectList(wrapper);
            if (ObjectUtils.isNotEmpty(books)) {
                throw new BusinessException(StandardErrorCode.USED_BY_BOOK, books.get(0).getName());
            }
        }

        Standard standard = new Standard();

        BeanUtil.copyProperties(dto, standard);

        boolean result = super.updateById(standard);

        return result ? new Message<>(Message.SUCCESS, "修改成功") : new Message<>(Message.FAIL, "修改失败");
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();
        //删除检查是否禁用
        LambdaUpdateWrapper<Standard> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Standard::getId, listIds);
        wrapper.eq(Standard::getStatus, 1);
        List<Standard> list = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(list)) {
            throw new BusinessException(BookBusinessExceptionEnum.DISABLE_BEFORE_DELETE);
        }

        //删除制度科目
        standardSubjectMapper.delete(
        		Wrappers.<StandardSubject>lambdaQuery()
                	.in(StandardSubject::getStandardId, listIds));
        boolean result = super.removeByIds(listIds);

        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }
}
