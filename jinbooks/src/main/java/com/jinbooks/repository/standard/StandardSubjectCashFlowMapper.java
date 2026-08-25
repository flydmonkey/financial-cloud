package com.jinbooks.repository.standard;

import com.jinbooks.repository.standard.StandardSubjectCashFlowMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.standard.StandardSubjectCashFlow;
import com.jinbooks.dto.standard.StandardSubjectCashFlowDto;
import com.jinbooks.dto.standard.StandardSubjectCashFlowVo;
import com.jinbooks.domain.voucher.VoucherItemCashFlow;
import com.jinbooks.dto.voucher.VoucherChangeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StandardSubjectCashFlowMapper extends BaseMapper<StandardSubjectCashFlow> {
    StandardSubjectCashFlowVo fetchRelationships(@Param("dto") StandardSubjectCashFlowDto dto);

    List<VoucherItemCashFlow> getSubjectCashFlow(@Param("dto") VoucherChangeDto dto);
}
