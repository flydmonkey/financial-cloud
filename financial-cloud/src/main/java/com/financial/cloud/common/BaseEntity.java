package com.financial.cloud.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import com.financial.cloud.util.DateUtils;
import com.financial.cloud.util.excel.ExcelExportCfg;
import lombok.Data;

@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public BaseEntity(){

    }
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelExportCfg(dateFormat = DateUtils.FORMAT_DATE_YYYY_MM_DD_HH_MM_SS)
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
    @ExcelExportCfg(dateFormat = DateUtils.FORMAT_DATE_YYYY_MM_DD_HH_MM_SS)
    protected Date modifiedDate;

}
