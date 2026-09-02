package com.financial.cloud.domain.fixedasset;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fixed_asset_change_item")
public class FixedAssetChangeItem extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String bookId;
    private String changeId;
    private String assetId;
    private String fieldCode;
    private String fieldLabel;
    private String beforeValue;
    private String afterValue;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
