/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

package com.jinbooks.domain.standard;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报表统计规则实体对象 jbx_standard_statement_rules
 *
 * @author wuyan
 * {@code @date} 2025-03-19
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("jbx_standard_statement_rules")
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
