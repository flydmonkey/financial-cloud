package com.financial.cloud.dto.voucher;

import com.financial.cloud.domain.voucher.VoucherTemplateItem;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.util.List;

@Data
public class VoucherTemplateChangeDto {
    /**
     * 主键
     */
    @NotNull(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    String id;
    
    @NotEmpty(message = MessageKeys.Validation.COMMON_TEMPLATE_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    String name;
    
    String code;
    
    Integer category;
    /**
     * 字头：“收”、“付”、“转”等
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_VOUCHER_PREFIX_REQUIRED, groups = {AddGroup.class})
    String wordHead;

    /**
     * 默认凭证日期，为月份的第几天，0为月末
     */
    Integer voucherDate;
    
    Integer voucherType;

    /**
     * 所属账套
     */
    @NotEmpty(message = MessageKeys.Validation.COMMON_RELATED_OBJECT_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    String relatedId;

    /**
     * 备注
     */
    String remark;
    /**
     * 排序
     */
    Integer sortIndex;
    
    /**
     * 状态：暂存 - draft,审核中 - reviewing，已完成 - completed，被拒绝 - rejected，已取消 - cancelled
     */
    String status;

    /**
     * 凭证明细记录
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_VOUCHER_ITEMS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private List<VoucherTemplateItem> items;
}
