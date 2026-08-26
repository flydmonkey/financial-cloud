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
import java.util.List;

/**
 * èµäº§è´åºè¡¨ statement_balance_sheet
 *
 * @author wuyan
 * {@code @date} 2025-02-03
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("statement_balance_sheet_item")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceSheetItem extends BaseEntity implements Serializable {

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

    @NotBlank(message = "ä¸»æ¥è¡¨ID", groups = {AddGroup.class, EditGroup.class})
    private String balanceSheetId;

    /**
     * æ¯èµäº§è¿æ¯è´åºï¼asset, liability
     */
    @NotBlank(message = "é¡¹ç±»åï¼èµäº§ãè´åºï¼ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String assetOrLiability;

    /**
     * ç¼ç 
     */
    private String itemCode;

    /**
     * è´¢å¡é¡¹ç®çåç§?
     */
    @NotBlank(message = "è´¢å¡é¡¹ç®ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String itemName;

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
     * ç¶çº§ç¼ç 
     */
    private String parentItemCode;

    /**
     * è®¡ç®è§åï¼?ï¼?
     */
    private String symbol;

    /**
     * åæ°è§åï¼æ ¹æ®ç§ç®åæ?ï¼èªå®ä¹è¾å¥2
     */
    private String rule;

    /**
     * ææ«ä½é¢
     */
    private BigDecimal currentBalance;

    /**
     * å¹´åä½é¢
     */
    private BigDecimal initialBalance;

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
