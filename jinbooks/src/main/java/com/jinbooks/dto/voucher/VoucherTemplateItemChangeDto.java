package com.jinbooks.dto.voucher;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinbooks.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 凭证记录编辑对象
 *
 * @author wuyan
 * {@code @date} 2025-01-14
 */

@Data
public class VoucherTemplateItemChangeDto {
    /**
     * 主键
     */
    @NotNull(message = "编辑对象不能为空", groups = {EditGroup.class})
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
    @NotNull(message = "编辑摘要不能为空", groups = {EditGroup.class})
    String summary;
    
    /**
     * 科目编码
     */
    @NotNull(message = "编辑科目不能为空", groups = {EditGroup.class})
    String subjectCode;
    
    /**
     * 余额方向 1 借 2 贷
     */
    @NotNull(message = "编辑借贷方向不能为空", groups = {EditGroup.class})
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Integer direction;
}
