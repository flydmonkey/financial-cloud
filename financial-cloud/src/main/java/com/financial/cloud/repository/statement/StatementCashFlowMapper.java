package com.financial.cloud.repository.statement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.statement.StatementCashFlow;
import com.financial.cloud.dto.voucher.VoucherItemPageDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatementCashFlowMapper extends BaseMapper<StatementCashFlow> {
    List<VoucherItemVo> fetchByCashFlow(@Param("params") VoucherItemPageDto params);

    List<VoucherItemVo> fetchByCashFlowAccumulated(@Param("params") VoucherItemPageDto params);

    List<StatementCashFlow> fetchSpecifyCashFlow(@Param("params") VoucherItemPageDto params);
}
