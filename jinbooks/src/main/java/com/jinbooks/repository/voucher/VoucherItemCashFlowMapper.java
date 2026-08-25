package com.jinbooks.repository.voucher;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.voucher.VoucherItemCashFlow;
import com.jinbooks.dto.voucher.VoucherItemPageDto;
import com.jinbooks.dto.voucher.VoucherItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VoucherItemCashFlowMapper extends BaseMapper<VoucherItemCashFlow> {
    List<VoucherItemVo> getCashFlowItems(@Param("params") VoucherItemPageDto params);

    List<String> getIdsWhenDelete(@Param("params") VoucherItemPageDto params);
}
