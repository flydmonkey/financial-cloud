package com.financial.cloud.dto.voucher;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

@Data
public class VoucherTemplateItemChangeDto {
    /**
     * 主键
     */
    @NotNull(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    private String id;

    /**
     * 关联编码
     */
    String relatedId;
    /**
     * 模板名称
     */
    String templateId;

    /**
     * 摘要
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_EDIT_SUMMARY_REQUIRED, groups = {EditGroup.class})
    String summary;
    
    /**
     * 科目编码
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_EDIT_SUBJECT_REQUIRED, groups = {EditGroup.class})
    String subjectCode;
    
    /**
     * 余额方向 1 借 2 贷
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_EDIT_DEBIT_CREDIT_REQUIRED, groups = {EditGroup.class})
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Integer direction;
}
