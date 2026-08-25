package com.jinbooks.repository.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.book.Settlement;
import com.jinbooks.dto.book.SettlementPageDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:12
 */

@Mapper
public interface SettlementMapper extends BaseMapper<Settlement> {
    Page<Settlement> pageList(Page page, @Param("dto") SettlementPageDto dto);
}
