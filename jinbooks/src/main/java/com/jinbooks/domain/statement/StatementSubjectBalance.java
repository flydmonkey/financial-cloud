package com.jinbooks.domain.statement;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ç§ç®ä½é¢ jbx_statement_subject_balance
 *
 * @author wuyan
 * {@code @date} 2025-02-03
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("jbx_statement_subject_balance")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementSubjectBalance extends BaseEntity implements Serializable {

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
    private String bookId;
    private String yearPeriod;

    /**
     * æ¥è¡¨å¨æï¼å¦ï¼monthãquarterãyearï¼?
     */
    private String periodType;

    /**
     * æåºåºå·
     */
    private Integer sortIndex;

    /**
     * åå§æ°æ®id
     */
    private String sourceId;

    /**
     * ç¶çº§ç§ç®id
     */
    private String parentId;

    /**
     * ç§ç®ç¼ç 
     */
    private String subjectCode;

    /**
     * ç§ç®åç§°
     */
    private String subjectName;

    /**
     * åè´·æ¹å
     */
    String direction;

    /**
     * ä½é¢
     */
    private BigDecimal balance;

    /**
     * æåä½é¢ï¼åæ¹ï¼?
     */
    private BigDecimal openingBalanceDebit;

    /**
     * æåä½é¢ï¼è´·æ¹ï¼
     */
    private BigDecimal openingBalanceCredit;

    /**
     * å¹´åä½é¢ï¼åæ¹ï¼?
     */
    private BigDecimal openingYearBalanceDebit;

    /**
     * å¹´åä½é¢ï¼è´·æ¹ï¼
     */
    private BigDecimal openingYearBalanceCredit;

    /**
     * æ¬æåçé¢ï¼åæ¹ï¼?
     */
    private BigDecimal currentPeriodDebit;

    /**
     * æ¬æåçé¢ï¼è´·æ¹ï¼?
     */
    private BigDecimal currentPeriodCredit;

    /**
     * æ¬å¹´ç´¯è®¡åçé¢ï¼åæ¹ï¼?
     */
    private BigDecimal yearToDateDebit;

    /**
     * æ¬å¹´ç´¯è®¡åçé¢ï¼è´·æ¹ï¼?
     */
    private BigDecimal yearToDateCredit;

    /**
     * ææ«ä½é¢ï¼åæ¹ï¼?
     */
    private BigDecimal closingBalanceDebit;

    /**
     * ææ«ä½é¢ï¼è´·æ¹ï¼
     */
    private BigDecimal closingBalanceCredit;

    /**
     * ä¸ææ«ä½é¢?
     */
    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevBalance;
    

    /**
     * ä¸æææ«ä½é¢ï¼åæ¹ï¼?
     */
    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevClosingBalanceDebit;

    /**
     * ä¸æææ«ä½é¢ï¼è´·æ¹ï¼
     */
    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevClosingBalanceCredit;
    
    /**
     * ä¸ææ¬å¹´ç´¯è®¡åçé¢ï¼åæ¹ï¼?
     */
    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevYearToDateDebit;

    /**
     * ä¸ææ¬å¹´ç´¯è®¡åçé¢ï¼è´·æ¹ï¼?
     */
    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevYearToDateCredit;
    
    /**
     * æ¯å¦è¾å©æ ¸ç®é¡?n-å?y-æ?
     */
    private String isAuxiliary;

    /**
     * å½åææ¯å¦ä½¿ç¨ï¼n-å?y-æ?
     */
    private String isVoucher;

    /**
     * å é¤æ è®°ï¼é»è®¤ä¸º 'n' (æªå é?ï¼å¦æä¸º 'y' è¡¨ç¤ºå·²å é?
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
