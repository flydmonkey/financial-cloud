package com.financial.cloud.repository.expense;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.expense.ExpenseClaim;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用报销单 Mapper。
 */
@Mapper
public interface ExpenseClaimMapper extends BaseMapper<ExpenseClaim> {
}
