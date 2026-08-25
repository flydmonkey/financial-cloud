package com.jinbooks.repository.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.book.SettlementCarryforward;
import com.jinbooks.dto.book.SettlementCarryforwardVo;
import com.jinbooks.dto.voucher.VoucherTemplatePageDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettlementCarryforwardMapper extends BaseMapper<SettlementCarryforward> {
    Page<SettlementCarryforwardVo> pageList(Page page, @Param("dto") VoucherTemplatePageDto dto);
}
