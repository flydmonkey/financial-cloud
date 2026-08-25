package com.jinbooks.domain.voucher;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinbooks.enums.VoucherStatusEnum;
import com.jinbooks.util.excel.ExcelExportCfg;
import lombok.*;
import com.jinbooks.common.BaseEntity;

import java.io.Serializable;
import java.util.List;

/**
 * 凭证模板 voucher_template
 *
 * @author wuyan
 * {@code @date} 2025-05-08
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("voucher_template")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherTemplate extends BaseEntity implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -3028372120266615619L;

	/**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    String id;

    /**
     * 关联编码
     */
    String relatedId;

    String code;
    
    /**
     * 名称
     */
    String name;
    /**
     * 类型 1 期末处理 2 薪资 3 日记账
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Integer category;
    
    /**
     * 
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Integer voucherType;
    
    /**
     * 默认凭证日期，为月份的第几天，0为月末
     */
    Integer voucherDate;
    
    /**
     * 字头：“记”、“收”、“付”、“转”等
     */
    String wordHead;

    /**
     * 备注
     */
    String remark;
    /**
     * 排序
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Integer sortIndex;
    
    /**
     * 状态
     */
    @ExcelExportCfg(enumClass = VoucherStatusEnum.class)
    String status;



    /**
     * 删除标记
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
    
    
    /**
     * 模板的条目
     */
    @TableField(exist = false)
    private List<VoucherTemplateItem> items;
}
