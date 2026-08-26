package com.financial.cloud.dto.voucher;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

import com.financial.cloud.domain.voucher.Voucher;

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
