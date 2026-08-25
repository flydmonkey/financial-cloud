package com.jinbooks.dto.voucher;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

import com.jinbooks.domain.voucher.Voucher;

/**
 * 凭证记录视图对象
 *
 * @author wuyan
 * {@code @date} 2025-01-14
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class VoucherVo extends Voucher {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 制单人名称
     */
    private String createdName;

    /**
     * 凭证明细记录
     */
    private List<VoucherItemVo> items;
}
