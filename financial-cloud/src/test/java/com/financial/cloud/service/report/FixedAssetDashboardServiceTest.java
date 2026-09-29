package com.financial.cloud.service.report;

import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.dto.report.FixedAssetCountVo;
import com.financial.cloud.enums.fixedasset.FixedAssetStatus;
import com.financial.cloud.repository.fixedasset.FixedAssetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedAssetDashboardServiceTest {

    @Mock
    private FixedAssetMapper fixedAssetMapper;

    @InjectMocks
    private FixedAssetDashboardService service;

    @Test
    void statisticsAssetCount_sumsNonDisposed() {
        FixedAsset inUse = FixedAsset.builder()
                .id("a1")
                .status(FixedAssetStatus.IN_USE.name())
                .originalValue(new BigDecimal("10000"))
                .accumDepr(new BigDecimal("2000"))
                .impairment(new BigDecimal("500"))
                .build();
        FixedAsset suspended = FixedAsset.builder()
                .id("a2")
                .status(FixedAssetStatus.SUSPENDED.name())
                .originalValue(new BigDecimal("4000"))
                .accumDepr(BigDecimal.ZERO)
                .build();
        when(fixedAssetMapper.selectList(any())).thenReturn(List.of(inUse, suspended));

        FixedAssetCountVo vo = service.statisticsAssetCount("book-1");

        assertEquals(2, vo.getTotalCount());
        assertEquals(1, vo.getInUseCount());
        assertEquals(1, vo.getSuspendedCount());
        assertEquals(0, new BigDecimal("14000").compareTo(vo.getOriginalValueSum()));
        assertEquals(0, new BigDecimal("11500").compareTo(vo.getNetValueSum()));
    }

    @Test
    void statisticsAssetCount_blankBookReturnsZeros() {
        FixedAssetCountVo vo = service.statisticsAssetCount(" ");
        assertEquals(0, vo.getTotalCount());
        assertEquals(0, new BigDecimal("0").compareTo(vo.getOriginalValueSum()));
    }
}
