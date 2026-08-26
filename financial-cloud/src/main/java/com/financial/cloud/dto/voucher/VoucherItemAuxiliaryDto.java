package com.financial.cloud.dto.voucher;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.financial.cloud.constants.common.MessageKeys;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherItemAuxiliaryDto {
    /**
     * 辅助核算对象类型
     */
    @NotNull(message = MessageKeys.Validation.ASSIST_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String id;

    /**
     * 辅助核算对象名称
     */
    @NotNull(message = MessageKeys.Validation.ASSIST_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String label;

    /**
     * 辅助核算对象列表
     */
    private List<BooksVoucherItemAuxiliaryValue> value;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BooksVoucherItemAuxiliaryValue {
        /**
         * 对象名称
         */
        @NotNull(message = MessageKeys.Validation.ASSIST_TARGET_REQUIRED, groups = {AddGroup.class, EditGroup.class})
        private String label;

        /**
         * 对象ID
         */
        @NotNull(message = MessageKeys.Validation.ASSIST_TARGET_REQUIRED, groups = {AddGroup.class, EditGroup.class})
        private String value;
    }
}
