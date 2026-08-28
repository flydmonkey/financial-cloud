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
     * 按科目×月聚合费用类科目借贷发生额
     *
     * @param params   查询参数
     * @param prefixes 科目编码前缀列表
     * @return 每行 subjectCode、yearPeriod、debitAmount、creditAmount
     */
    List<VoucherItemVo> selectExpenseAmountByMonth(
            @Param("params") StatementParamsDto params,
            @Param("prefixes") List<String> prefixes);

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
