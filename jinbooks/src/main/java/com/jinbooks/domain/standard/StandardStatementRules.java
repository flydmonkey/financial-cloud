package com.jinbooks.domain.standard;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报表统计规则实体对象 standard_statement_rules
 *
 * @author wuyan
 * {@code @date} 2025-03-19
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("standard_statement_rules")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardStatementRules extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotBlank(message = "准则编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String standardId;
    
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

}
