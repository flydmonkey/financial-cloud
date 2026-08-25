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
 

package com.jinbooks.domain.statement;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinbooks.common.BaseEntity;
import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ç°éæµéè¡?jbx_statement_cash_flow
 *
 * @author wuyan
 * {@code @date} 2025-02-03
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("jbx_statement_cash_flow")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementCashFlow extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ä¸»é®
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * æ¥è¡¨æ¥æï¼ææ«æ¥æï¼
     */
    @TableField(exist = false)
    @NotBlank(message = "æé´ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String yearPeriod;

    /**
     * ä¸å¡æ¥æ
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;


    /**
     * æ¥è¡¨å¨æï¼å¦ï¼æåº¦ãå­£åº¦ãå¹´åº¦ï¼
     */
    private String periodType;

    /**
     * æåºåºå·
     */
    private Integer sortIndex;

    /**
     * è´¢å¡é¡¹ç®çåç§?
     */
    private String itemName;

    /**
     * è´¢å¡é¡¹ç®çcode
     */
    private String itemCode;

    /**
     * æ¬å¹´ç´¯è®¡éé¢
     */
    private BigDecimal currentAmount;

    /**
     * æ¬æéé¢
     */
    private BigDecimal monthlyAmount;

    /**
     * æ¬æéé¢
     */
    private String bookId;


    /**
     * å é¤æ è®°ï¼é»è®¤ä¸º 'n' (æªå é?ï¼å¦æä¸º 'y' è¡¨ç¤ºå·²å é?
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private Integer isTitle;

    @TableField(exist = false)
    private Integer isMain;

    @TableField(exist = false)
    private Integer isAdditional;

    /**
     * æ¯å¦ä¸ºè®¡ç®è¡ 0-å?1-æ?
     */
    @TableField(exist = false)
    private Integer isResult;
}
