package com.financial.cloud.common;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/1/17 16:38
 */

@Data
public class BaseSubject implements Serializable {
    @Serial
    private static final long serialVersionUID = -8364438812300012091L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    /**
     * ç§ç±»
     */
    Integer category;

    /**
     * ç§ç®ç¼ç 
     */
    String code;

    /**
     * ç§ç®åç§°
     */
    String name;

    /**
     * ç§ç®å¨ç§°
     */
    String displayName;

    /**
     * æ¼é³ç¼ç 
     */
    String pinyinCode;
    /**
     * æ¼é³å¨ç§°
     */
    String pinyinDisplayCode;
    /**
     * åè´·æ¹å
     */
    String direction;

    Integer status;

    String parentId;

    String idPath;

    Integer level;

    Integer systemDefault;

    String unit;
    /**
     * è¾å©æ ¸ç®
     */
    String auxiliary;
    /**
     * å¸ç§
     */
    String currency;
    /**
     * ä½¿ç¨èå´
     */
    String scope;
    /**
     * åç±»
     */
    String classify;

    /**
     * æ¯å¦ä¸ºç°éç±»ç§ç®
     */
    Integer isCash;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;

    @TableField(exist = false)
    String parentCode;

    @TableField(exist = false)
    String parentName;
    
    @TableField(exist = false)
    BigDecimal balance;

    /**
     * åå»ºè?
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    protected String createdBy;

    /**
     * åå»ºæ¶é´
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    protected Date createdDate;

    /**
     * æ´æ°è?
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    protected String modifiedBy;

    /**
     * æ´æ°æ¶é´
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    protected Date modifiedDate;
}
