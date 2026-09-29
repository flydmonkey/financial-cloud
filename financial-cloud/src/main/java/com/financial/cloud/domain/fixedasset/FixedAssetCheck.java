package com.financial.cloud.domain.fixedasset;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 资产盘点单（头）：一次盘点任务的快照与结果统计。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fixed_asset_check")
public class FixedAssetCheck extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 盘点中 */
    public static final String STATUS_DRAFT = "draft";
    /** 已完成 */
    public static final String STATUS_COMPLETED = "completed";

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String bookId;

    /** 盘点单标题 */
    private String title;

    /** 盘点日期 */
    private Date checkDate;

    /** draft / completed */
    private String status;

    /** 应盘资产数 */
    private Integer totalCount;

    /** 正常数 */
    private Integer normalCount;

    /** 盘盈数（实盘 > 账面） */
    private Integer surplusCount;

    /** 盘亏数（实盘 < 账面） */
    private Integer deficitCount;

    /**
     * 尚未盘盈入账的明细数（非表字段；列表接口填充，用于隐藏「盘盈入账」按钮）。
     */
    @TableField(exist = false)
    private Integer pendingSurplusCount;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
