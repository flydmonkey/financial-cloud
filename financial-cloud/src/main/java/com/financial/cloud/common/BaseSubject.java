package com.financial.cloud.common;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class BaseSubject implements Serializable {
    @Serial
    private static final long serialVersionUID = -8364438812300012091L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    Integer category;

    String code;

    String name;

    String displayName;

    String pinyinCode;
    String pinyinDisplayCode;
    String direction;

    Integer status;

    String parentId;

    String idPath;

    Integer level;

    Integer systemDefault;

    String unit;
    String auxiliary;
    String currency;
    String scope;
    String classify;

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

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    protected String createdBy;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    protected Date createdDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    protected String modifiedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    protected Date modifiedDate;
}
