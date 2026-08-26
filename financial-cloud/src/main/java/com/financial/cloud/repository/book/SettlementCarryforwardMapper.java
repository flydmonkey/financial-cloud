package com.financial.cloud.repository.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.book.SettlementCarryforward;
import com.financial.cloud.dto.book.SettlementCarryforwardVo;
import com.financial.cloud.dto.voucher.VoucherTemplatePageDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettlementCarryforwardMapper extends BaseMapper<SettlementCarryforward> {
    Page<SettlementCarryforwardVo> pageList(Page page, @Param("dto") VoucherTemplatePageDto dto);
}
