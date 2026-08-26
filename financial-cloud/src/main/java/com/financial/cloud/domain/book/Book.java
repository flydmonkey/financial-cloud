package com.financial.cloud.domain.book;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.YearMonth;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 9:54
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("book")
public class Book extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 4825104334666554378L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    String name;

    String companyName;

    @JsonFormat(pattern="yyyy-MM")
    YearMonth enableDate;

    String creditCode;

    String standardId;

    String address;

    Integer industry;

    Integer vatType;

    Integer voucherReviewed;

    @JsonFormat(pattern="yyyy-MM")
    YearMonth currentAccountDate;

    Integer status;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;

    @TableField(exist = false)
    String standardsName;
    
    @TableField(exist = false)
    String bookId;
}
