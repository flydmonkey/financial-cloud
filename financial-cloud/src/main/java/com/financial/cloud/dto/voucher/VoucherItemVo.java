package com.financial.cloud.dto.voucher;

import com.financial.cloud.domain.voucher.VoucherItem;
import com.financial.cloud.dto.voucher.VoucherItemAuxiliaryDto;

import lombok.*;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;

/**
 * 凭证明细视图对象
 *
 * @author wuyan
 * {@code @date} 2025-01-14
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class VoucherItemVo extends VoucherItem {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 凭证字号
     */
    private String word;

    /**
     * 辅助核算配置
     */
    private List<VoucherItemAuxiliaryDto> auxiliary;

    /**
     * 辅助核算显示名称
     */
    private String auxiliaryLabel;

    /**
     * 现金流量项
     */
    private String cashFlowItemCode;

    /**
     * 现金流量额
     */
    private BigDecimal cashFlowBalance;

    /**
     * 凭证项目Id
     */
    private String voucherItemId;

    /**
     * 凭证项目类型
     */
    private Integer cashFlowItemType;

    private Integer entryNo;
}
