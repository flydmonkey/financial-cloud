package com.jinbooks.repository.statement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.statement.StatementCashFlow;
import com.jinbooks.dto.voucher.VoucherItemPageDto;
import com.jinbooks.dto.voucher.VoucherItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatementCashFlowMapper extends BaseMapper<StatementCashFlow> {
    List<VoucherItemVo> fetchByCashFlow(@Param("params") VoucherItemPageDto params);

    List<VoucherItemVo> fetchByCashFlowAccumulated(@Param("params") VoucherItemPageDto params);

    List<StatementCashFlow> fetchSpecifyCashFlow(@Param("params") VoucherItemPageDto params);
}
