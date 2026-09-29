package com.financial.cloud.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSettlementParamsVo {
    private Integer voucherReviewed;
    private boolean arapVerifyEnabled;
    /** 往来逾期是否硬阻断结账；缺省 false */
    private boolean arapOverdueHard;
    private boolean canEdit;
    private List<String> hardGateLabels;
}
