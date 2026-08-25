package com.jinbooks.domain.statement;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import com.jinbooks.dto.statement.StatementBalanceSheetItemListVo;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * èµäº§è´åºè¡¨ jbx_statement_balance_sheet
 *
 * @author wuyan
 * {@code @date} 2025-02-03
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("jbx_statement_balance_sheet")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceSheet extends BaseEntity implements Serializable {

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

   
    @NotBlank(message = "æé´ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String yearPeriod;

    /**
     * æ¥è¡¨å¨æï¼å¦ï¼monthãquarterãyearï¼?
     */
    @NotBlank(message = "报表周期（月、季、年）不能为空", groups = {AddGroup.class, EditGroup.class})
    private String periodType;

    /**
     * å é¤æ è®°ï¼é»è®¤ä¸º 'n' (æªå é?ï¼å¦æä¸º 'y' è¡¨ç¤ºå·²å é?
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    /**
     * æç»é¡?
     */
    @TableField(exist = false)
    private StatementBalanceSheetItemListVo items;

}
