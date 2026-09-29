package com.financial.cloud.repository.expense;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.expense.ExpenseClaimAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报销单票据附件 Mapper。
 */
@Mapper
public interface ExpenseClaimAttachmentMapper extends BaseMapper<ExpenseClaimAttachment> {
}
