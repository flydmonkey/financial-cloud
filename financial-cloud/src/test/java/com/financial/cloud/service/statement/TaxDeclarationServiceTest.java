package com.financial.cloud.service.statement;

import com.financial.cloud.dto.statement.TaxDeclarationVo;
import com.financial.cloud.dto.statement.TaxEstimateVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxDeclarationServiceTest {

    private static final String BOOK_ID = "book-test-1";

    @Mock
    private TaxEstimateService taxEstimateService;

    @InjectMocks
    private TaxDeclarationService service;

    private TaxEstimateVo estimate(String vatPayable, String vatCredit, String vatDue) {
        return TaxEstimateVo.builder()
                .yearMonth("2026-09")
                .outputTax(new BigDecimal("13000.00"))
                .inputTax(new BigDecimal("8000.00"))
                .inputTransferOut(new BigDecimal("1000.00"))
                .paidTax(new BigDecimal("2000.00"))
                .vatPayable(new BigDecimal(vatPayable))
                .vatCredit(new BigDecimal(vatCredit))
                .vatDue(new BigDecimal(vatDue))
                .urbanRate(new BigDecimal("0.07"))
                .urbanTax(new BigDecimal("280.00"))
                .eduRate(new BigDecimal("0.03"))
                .eduTax(new BigDecimal("120.00"))
                .localEduRate(new BigDecimal("0.02"))
                .localEduTax(new BigDecimal("80.00"))
                .surtaxTotal(new BigDecimal("480.00"))
                .profitBeforeTax(new BigDecimal("50000.00"))
                .incomeTaxRate(new BigDecimal("0.25"))
                .incomeTax(new BigDecimal("12500.00"))
                .revenue(new BigDecimal("100000.00"))
                .build();
    }

    private Map<String, TaxDeclarationVo.Line> byRowNo(List<TaxDeclarationVo.Line> lines) {
        return lines.stream().collect(Collectors.toMap(TaxDeclarationVo.Line::getRowNo, l -> l, (a, b) -> a));
    }

    private void stubEstimate(TaxEstimateVo vo) {
        when(taxEstimateService.estimate(eq(BOOK_ID), eq("2026-09"), any(), any(), any(), any(), any()))
                .thenReturn(vo);
    }

    @Test
    void declarationMapsEstimateToOfficialRowNumbers() {
        stubEstimate(estimate("6000.00", "0.00", "4000.00"));

        TaxDeclarationVo vo = service.declaration(BOOK_ID, "2026-09",
                new BigDecimal("0.07"), new BigDecimal("0.03"), new BigDecimal("0.02"), new BigDecimal("0.25"));

        assertEquals("2026-09", vo.getYearMonth());
        assertEquals(20, vo.getLines().size());
        Map<String, TaxDeclarationVo.Line> rows = byRowNo(vo.getLines());
        assertEquals(new BigDecimal("100000.00"), rows.get("1").getAmount());
        assertEquals(new BigDecimal("13000.00"), rows.get("11").getAmount());
        assertEquals(new BigDecimal("8000.00"), rows.get("12").getAmount());
        assertEquals(new BigDecimal("1000.00"), rows.get("14").getAmount());
        // 应抵扣合计 = 进项 - 进项转出
        assertEquals(new BigDecimal("7000.00"), rows.get("17").getAmount());
        // 应纳税额为正 → 实际抵扣 = 应抵扣合计
        assertEquals(new BigDecimal("7000.00"), rows.get("18").getAmount());
        assertEquals(new BigDecimal("6000.00"), rows.get("19").getAmount());
        assertEquals(new BigDecimal("0.00"), rows.get("20").getAmount());
        assertEquals(new BigDecimal("2000.00"), rows.get("27").getAmount());
        assertEquals(new BigDecimal("4000.00"), rows.get("34").getAmount());
        // 账面无法拆分的行次留空
        assertNull(rows.get("5").getAmount());
        assertNull(rows.get("13").getAmount());
    }

    @Test
    void declarationClampsActualDeductToOutputWhenCredit() {
        // 应纳税额为负（留抵 3000）：实际抵扣 = 销项税额，期末留抵带出
        stubEstimate(estimate("-3000.00", "3000.00", "0.00"));

        TaxDeclarationVo vo = service.declaration(BOOK_ID, "2026-09",
                new BigDecimal("0.07"), new BigDecimal("0.03"), new BigDecimal("0.02"), new BigDecimal("0.25"));

        Map<String, TaxDeclarationVo.Line> rows = byRowNo(vo.getLines());
        assertEquals(new BigDecimal("13000.00"), rows.get("18").getAmount());
        assertEquals(new BigDecimal("0.00"), rows.get("19").getAmount());
        assertEquals(new BigDecimal("3000.00"), rows.get("20").getAmount());
        assertEquals(new BigDecimal("0.00"), rows.get("34").getAmount());
    }

    @Test
    void declarationRendersSurtaxAndIncomeTaxSectionsWithRates() {
        stubEstimate(estimate("6000.00", "0.00", "4000.00"));

        TaxDeclarationVo vo = service.declaration(BOOK_ID, "2026-09",
                new BigDecimal("0.07"), new BigDecimal("0.03"), new BigDecimal("0.02"), new BigDecimal("0.25"));

        List<TaxDeclarationVo.Line> surtax = vo.getLines().stream()
                .filter(l -> l.getSection().startsWith("四、"))
                .toList();
        assertEquals(4, surtax.size());
        assertEquals("城市维护建设税（7%）", surtax.get(0).getItem());
        assertEquals(new BigDecimal("480.00"), surtax.get(3).getAmount());

        List<TaxDeclarationVo.Line> incomeTax = vo.getLines().stream()
                .filter(l -> l.getSection().startsWith("五、"))
                .toList();
        assertEquals(2, incomeTax.size());
        assertEquals("应纳所得税额（25%）", incomeTax.get(1).getItem());
        assertEquals(new BigDecimal("12500.00"), incomeTax.get(1).getAmount());
    }
}
