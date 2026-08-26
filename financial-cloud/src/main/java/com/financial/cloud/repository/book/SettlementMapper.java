package com.financial.cloud.repository.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.dto.book.SettlementPageDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettlementMapper extends BaseMapper<Settlement> {
    Page<Settlement> pageList(Page page, @Param("dto") SettlementPageDto dto);
}
