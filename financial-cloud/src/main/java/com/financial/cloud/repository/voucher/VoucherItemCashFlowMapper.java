package com.financial.cloud.repository.voucher;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.voucher.VoucherItemCashFlow;
import com.financial.cloud.dto.voucher.VoucherItemPageDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VoucherItemCashFlowMapper extends BaseMapper<VoucherItemCashFlow> {
    List<VoucherItemVo> getCashFlowItems(@Param("params") VoucherItemPageDto params);

    List<String> getIdsWhenDelete(@Param("params") VoucherItemPageDto params);
}
