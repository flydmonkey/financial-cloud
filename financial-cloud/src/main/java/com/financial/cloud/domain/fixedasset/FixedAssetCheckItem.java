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

/**
 * 资产盘点明细：盘点时按在册资产快照生成，实盘录入后出结果。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fixed_asset_check_item")
public class FixedAssetCheckItem extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 账实相符 */
    public static final String RESULT_NORMAL = "normal";
    /** 盘盈（实盘 > 账面） */
    public static final String RESULT_SURPLUS = "surplus";
    /** 盘亏（实盘 < 账面） */
    public static final String RESULT_DEFICIT = "deficit";

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String bookId;

    private String checkId;

    private String assetId;

    /** 快照：资产编码 */
    private String assetCode;

    /** 快照：资产名称 */
    private String assetName;

    /** 快照：账面存放地 */
    private String location;

    /** 快照：账面数量 */
    private Integer bookQuantity;

    /** 实盘数量 */
    private Integer actualQuantity;

    /** 实盘存放地（不一致时记录） */
    private String actualLocation;

    /** normal / surplus / deficit（完成盘点时按实盘数自动判定） */
    private String result;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
