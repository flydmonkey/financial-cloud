package com.financial.cloud.util.excel;

import com.financial.cloud.enums.common.BaseEnum;
import com.financial.cloud.util.DateUtils;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ExcelExportCfg {
    /**
     * Excel 列名（表头）
     */
    String name() default "";

    /**
     * 列排序，值越小越靠前
     */
    int order() default Integer.MAX_VALUE;

    /**
     * 列宽
     */
    int width() default 20;

    /**
     * 日期格式，仅适用于 Date 类型
     */
    String dateFormat() default DateUtils.FORMAT_DATE_YYYY_MM_DD_HH_MM_SS;

    /**
     * 数字格式，如 "#.##"
     */
    String numberFormat() default "#.##";

    /**
     * 0值处理格式，如 ""
     */
    String numberZeroFormat() default "";

    /**
     * 是否忽略该字段
     */
    boolean ignore() default false;

    /**
     * 是否换行显示
     */
    boolean wrapText() default false;

    /**
     * 是否自动合并单元格
     */
    boolean mergeCells() default false;

    /**
     * 映射为中文（如枚举类型）
     * 枚举类必须实现 BaseEnum 接口
     */
    Class<? extends BaseEnum> enumClass() default BaseEnum.Default.class;
}
