package com.financial.cloud.repository.expense;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.expense.ExpenseClaimItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用报销单明细 Mapper。
 */
@Mapper
public interface ExpenseClaimItemMapper extends BaseMapper<ExpenseClaimItem> {
}
