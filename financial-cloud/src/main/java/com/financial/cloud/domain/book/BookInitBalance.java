package com.financial.cloud.domain.book;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("book_init_balance")
public class BookInitBalance extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 所属账套
     */
    private String bookId;

    /**
     * 种类
     */
    Integer category;

    /**
     * 科目编码
     */
    String code;

    /**
     * 科目名称
     */
    String name;

    /**
     * 借贷方向
     */
    String direction;

    /**
     * 上级目录
     */
    String parentId;

    /**
     * 编码路径
     */
    String idPath;

    /**
     * 级别
     */
    Integer level;

    /**
     * 余额
     */
    BigDecimal balance;

    /**
     * 年初余额（借方）
     */
    BigDecimal openingYearBalanceDebit;

    /**
     * 年初余额（贷方）
     */
    BigDecimal openingYearBalanceCredit;

    /**
     * 本年累计借方总金额（元）
     */
    BigDecimal debitAmount;

    /**
     * 本年累计贷方总金额（元）
     */
    BigDecimal creditAmount;

    /**
     * 单位
     */
    String unit;

    /**
     * 是否为现金类科目
     */
    Integer isCash;

    /**
     * 辅助核算类型，存在则为辅助核算项
     */
    String assistType;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;

}
