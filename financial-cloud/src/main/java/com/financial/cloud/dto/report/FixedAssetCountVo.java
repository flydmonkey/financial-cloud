package com.financial.cloud.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 首页固定资产规模：在册张数与价值（排除已清理）。
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FixedAssetCountVo {

    /** 在册资产卡片数（非 DISPOSED） */
    private long totalCount;

    /** 正常使用 */
    private long inUseCount;

    /** 暂停计提 */
    private long suspendedCount;

    /** 原值合计 */
    private BigDecimal originalValueSum;

    /** 净值合计 = 原值 − 累计折旧 − 减值 */
    private BigDecimal netValueSum;
}
