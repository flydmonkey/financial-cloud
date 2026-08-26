package com.financial.cloud.domain.voucher;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;
import com.financial.cloud.common.BaseEntity;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("voucher_template_item")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherTemplateItem extends BaseEntity implements Serializable {


	/**
	 * 
	 */
	private static final long serialVersionUID = -2142434165037189016L;

	/**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    String id;
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
    String summary;
    
    /**
     * 科目编码
     */
    String subjectCode;
    
    /**
     * 余额方向 1 借 2 贷
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Integer direction;
    /**
     * 删除标记
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
