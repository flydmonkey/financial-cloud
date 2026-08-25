package com.jinbooks.domain.config;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/6 17:45
 */


@EqualsAndHashCode(callSuper = true)
@TableName("jbx_config_personal_tax")
@Data
public class ConfigPersonalTax extends BaseEntity {
    @Serial
    private static final long serialVersionUID = -686214470533448285L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    Integer level;

    Integer minNum;

    Integer maxNum;

    Integer taxRate;

    Double calculationDeduction;

    /**
     * 税率类型：0-工资；1-劳务报酬
     */
    Integer type;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    // 获取真实税率（将整数转换为小数）
    public double getRealRate() {
        return taxRate / 100.0;
    }
}
