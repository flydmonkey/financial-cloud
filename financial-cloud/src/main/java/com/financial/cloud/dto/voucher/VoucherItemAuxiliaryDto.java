package com.financial.cloud.dto.voucher;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简介说明: 辅助核算配置实体类
 *
 * @author wuyan
 * {@code @date} 2025/02/23 14:29:58
 * {@code @version} 1.0
 */


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherItemAuxiliaryDto {
    /**
     * 辅助核算对象类型
     */
    @NotNull(message = "辅助核算类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String id;

    /**
     * 辅助核算对象名称
     */
    @NotNull(message = "辅助核算名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String label;

    /**
     * 辅助核算对象列表
     */
    private List<BooksVoucherItemAuxiliaryValue> value;

    /**
     * 辅助核算对象配置信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BooksVoucherItemAuxiliaryValue {
        /**
         * 对象名称
         */
        @NotNull(message = "辅助核算对象不能为空", groups = {AddGroup.class, EditGroup.class})
        private String label;

        /**
         * 对象ID
         */
        @NotNull(message = "辅助核算对象不能为空", groups = {AddGroup.class, EditGroup.class})
        private String value;
    }
}
