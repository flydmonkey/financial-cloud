package com.financial.cloud.repository.standard;

import com.financial.cloud.repository.standard.StandardSubjectCashFlowMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.standard.StandardSubjectCashFlow;
import com.financial.cloud.dto.standard.StandardSubjectCashFlowDto;
import com.financial.cloud.dto.standard.StandardSubjectCashFlowVo;
import com.financial.cloud.domain.voucher.VoucherItemCashFlow;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StandardSubjectCashFlowMapper extends BaseMapper<StandardSubjectCashFlow> {
    StandardSubjectCashFlowVo fetchRelationships(@Param("dto") StandardSubjectCashFlowDto dto);

    List<VoucherItemCashFlow> getSubjectCashFlow(@Param("dto") VoucherChangeDto dto);
}
