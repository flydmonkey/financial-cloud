package com.jinbooks.domain.standard;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * å©æ¶¦è¡¨æ¨¡æ?jbx_standard_statement_income_item
 *
 * @author wuyan
 * {@code @date} 2025-02-03
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("jbx_standard_statement_income")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardStatementIncome extends BaseEntity implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1364790594151305735L;

	/**
     * ä¸»é®
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * è´¦å¥ID
     */
    @NotBlank(message = "ååä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String standardId;


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

    /**
     * æåºåºå·
     */
//    @NotNull(message = "æåºåºå·ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private Integer sortIndex;

    /**
     * çº§å«
     */
    private Integer level;

    /**
     * ç¶çº§ID
     */
    private String parentItemCode;

    @NotBlank(message = "è®¡ç®æ¹å¼(+,-)ä¸è½ä¸ºç©º", groups = {AddGroup.class, EditGroup.class})
    private String symbol;
    private String subjectFlag;
    
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
    private List<StandardStatementRules> rules;

}
