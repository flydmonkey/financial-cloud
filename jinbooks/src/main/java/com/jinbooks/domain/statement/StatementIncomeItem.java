package com.jinbooks.domain.statement;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * å©æ¶¦è¡?statement_income
 *
 * @author wuyan
 * {@code @date} 2025-02-03
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("statement_income_item")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementIncomeItem extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ä¸»é®
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * è´¦å¥ID
     */
    @NotBlank(message = "è´¦å¥ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String bookId;

    /**
     * ç¶çº§ID
     */
    private String incomeId;


    /**
     * è´¢å¡é¡¹ç®çåç§?
     */
    @NotBlank(message = "è´¢å¡é¡¹ç®ç¼ç ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String itemCode;
    
    /**
     * è´¢å¡é¡¹ç®çåç§?
     */
    @NotBlank(message = "è´¢å¡é¡¹ç®ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String itemName;

    @NotBlank(message = "è®¡ç®æ¹å¼ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String symbol;
    
    /**
     * çº§å«
     */
    private Integer level;

    /**
     * æåºåºå·
     */
//    @NotNull(message = "æåºåºå·ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private Integer sortIndex;
    
    /**
     * æ¬æéé¢
     */
    private BigDecimal currentBalance;

    /**
     * æ¬å¹´éé¢
     */
    private BigDecimal cumulativeBalance;

    /**
     * å é¤æ è®°ï¼é»è®¤ä¸º 'n' (æªå é?ï¼å¦æä¸º 'y' è¡¨ç¤ºå·²å é?
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    /**
     * è®¡ç®è§å
     */
    @TableField(exist = false)
    private List<StatementRules> rules;

}
