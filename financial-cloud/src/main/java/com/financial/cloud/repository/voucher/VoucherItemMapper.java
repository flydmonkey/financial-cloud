package com.financial.cloud.repository.voucher;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.domain.voucher.VoucherItem;
import com.financial.cloud.dto.voucher.VoucherItemPageDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VoucherItemMapper extends BaseMapper<VoucherItem> {
    /**
     * 统计各科目的借方和贷方金额
     *
     * @param params 查询参数
     * @return 结果
     */
    List<VoucherItemVo> selectSubjectAmount(@Param("params") StatementParamsDto params);

    /**
     * 凭证明细分页查询
     *
     * @param page 分页参数
     * @param params  查询参数
     */
    Page<VoucherItemVo> subLedgerPage(Page<VoucherItem> page,@Param("params")  VoucherItemPageDto params);

    Page<VoucherItemVo> fetchByCashFlow(Page<VoucherItem> page,@Param("params")  VoucherItemPageDto params);
    
    List<StatementSubjectBalance> voucherSubjectBalanceSummary(StatementParamsDto dto);
}
