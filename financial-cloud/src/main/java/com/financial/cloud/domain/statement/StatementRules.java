package com.financial.cloud.domain.statement;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 报表统计规则实体对象 statement_rules
 *
 * @author wuyan
 * {@code @date} 2025-03-19
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("statement_rules")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementRules extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 账套ID
     */
    @NotBlank(message = "账套不能为空", groups = {AddGroup.class, EditGroup.class})
    private String bookId;
    
    @NotBlank(message = "报表类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String type;

    @NotBlank(message = "报表类目不能为空", groups = {AddGroup.class, EditGroup.class})
    private String itemCode;

    @NotBlank(message = "科目代码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String subjectCode;

    @NotBlank(message = "取数规则不能为空", groups = {AddGroup.class, EditGroup.class})
    private String rule;

    @NotBlank(message = "计算方式(+,-)不能为空", groups = {AddGroup.class, EditGroup.class})
    private String symbol;

    /**
     * 期末余额
     */
    @TableField(exist = false)
    private BigDecimal closingBalance;

    /**
     * 年初余额
     */
    @TableField(exist = false)
    private BigDecimal openingYearBalance;
}
