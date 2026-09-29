package com.financial.cloud.service.report;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.dto.report.FixedAssetCountVo;
import com.financial.cloud.enums.fixedasset.FixedAssetStatus;
import com.financial.cloud.repository.fixedasset.FixedAssetMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页固定资产统计。
 */
@Service
@RequiredArgsConstructor
public class FixedAssetDashboardService {

    private final FixedAssetMapper fixedAssetMapper;

    public FixedAssetCountVo statisticsAssetCount(String bookId) {
        if (StringUtils.isBlank(bookId)) {
            return empty();
        }
        List<FixedAsset> assets = fixedAssetMapper.selectList(Wrappers.<FixedAsset>lambdaQuery()
                .eq(FixedAsset::getBookId, bookId)
                .and(w -> w.ne(FixedAsset::getStatus, FixedAssetStatus.DISPOSED.name())
                        .or().isNull(FixedAsset::getStatus)));

        long inUse = 0;
        long suspended = 0;
        BigDecimal originalSum = BigDecimal.ZERO;
        BigDecimal netSum = BigDecimal.ZERO;
        for (FixedAsset asset : assets) {
            FixedAssetStatus status = FixedAssetStatus.from(asset.getStatus());
            if (status == FixedAssetStatus.SUSPENDED) {
                suspended++;
            } else {
                inUse++;
            }
            BigDecimal original = nz(asset.getOriginalValue());
            BigDecimal accum = nz(asset.getAccumDepr());
            BigDecimal impairment = nz(asset.getImpairment());
            originalSum = originalSum.add(original);
            netSum = netSum.add(original.subtract(accum).subtract(impairment));
        }
        return FixedAssetCountVo.builder()
                .totalCount(assets.size())
                .inUseCount(inUse)
                .suspendedCount(suspended)
                .originalValueSum(originalSum)
                .netValueSum(netSum)
                .build();
    }

    private static FixedAssetCountVo empty() {
        return FixedAssetCountVo.builder()
                .totalCount(0)
                .inUseCount(0)
                .suspendedCount(0)
                .originalValueSum(BigDecimal.ZERO)
                .netValueSum(BigDecimal.ZERO)
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
