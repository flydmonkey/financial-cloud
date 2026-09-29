package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.statement.TaxEstimateVo;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxEstimateServiceTest {

    private static final String BOOK_ID = "book-test-1";

    @Mock
    private VoucherItemMapper voucherItemMapper;
    @Mock
    private BookSubjectMapper bookSubjectMapper;

    @InjectMocks
    private TaxEstimateService service;

    private static final BigDecimal URBAN = new BigDecimal("0.07");
    private static final BigDecimal EDU = new BigDecimal("0.03");
    private static final BigDecimal LOCAL = new BigDecimal("0.02");
    private static final BigDecimal CIT = new BigDecimal("0.25");
    private static final BigDecimal THRESHOLD = new BigDecimal("0.01");

    @Test
    void estimate_vatAndSurtaxAndIncomeTax() {
        stubSubjects();
        // 增值税：销项 1300，进项 800，进项转出 50，已交 100 → 应纳税 550，应补 450
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("2221"), any(), any()))
                .thenReturn(List.of(
                        taxItem("2221.01.02", "销项税额", null, "1300"),
                        taxItem("2221.01.01", "进项税额", "800", null),
                        taxItem("2221.01.04", "进项税额转出", null, "50"),
                        taxItem("2221.01.06", "已交税金", "100", null)));
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("2171"), any(), any()))
                .thenReturn(List.of());
        // 损益：收入 10000（贷），成本 6000（借），所得税费用 100（剔除）
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("5"), any(), any()))
                .thenReturn(List.of());
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("6"), any(), any()))
                .thenReturn(List.of(
                        plItem("6001", "主营业务收入", null, "10000"),
                        plItem("6401", "主营业务成本", "6000", null),
                        plItem("6801", "所得税费用", "100", null)));

        TaxEstimateVo vo = service.estimate(BOOK_ID, "2026-09", URBAN, EDU, LOCAL, CIT, THRESHOLD);

        assertEquals(0, vo.getOutputTax().compareTo(new BigDecimal("1300.00")));
        assertEquals(0, vo.getInputTax().compareTo(new BigDecimal("800.00")));
        assertEquals(0, vo.getInputTransferOut().compareTo(new BigDecimal("50.00")));
        assertEquals(0, vo.getPaidTax().compareTo(new BigDecimal("100.00")));
        assertEquals(0, vo.getVatPayable().compareTo(new BigDecimal("550.00")));
        assertEquals(0, vo.getVatDue().compareTo(new BigDecimal("450.00")));
        // 附加税 = 450 × (7%+3%+2%) = 54
        assertEquals(0, vo.getUrbanTax().compareTo(new BigDecimal("31.50")));
        assertEquals(0, vo.getSurtaxTotal().compareTo(new BigDecimal("54.00")));
        // 利润总额 = 10000 - 6000（剔除所得税费用）= 4000；企税 = 1000
        assertEquals(0, vo.getProfitBeforeTax().compareTo(new BigDecimal("4000.00")));
        assertEquals(0, vo.getIncomeTax().compareTo(new BigDecimal("1000.00")));
        // 税负率 = 450 / 10000 = 4.5% > 1% 阈值，不预警
        assertEquals(0, vo.getVatBurdenRate().compareTo(new BigDecimal("0.0450")));
        assertEquals(Boolean.FALSE, vo.getBurdenWarning());
    }

    @Test
    void estimate_inputExceedsOutput_creditAndWarning() {
        stubSubjects();
        // 进项大于销项 → 留抵 200，应补 0，税负率 0 < 阈值 → 预警
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("2221"), any(), any()))
                .thenReturn(List.of(
                        taxItem("2221.01.02", "销项税额", null, "300"),
                        taxItem("2221.01.01", "进项税额", "500", null)));
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("2171"), any(), any()))
                .thenReturn(List.of());
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("5"), any(), any()))
                .thenReturn(List.of());
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("6"), any(), any()))
                .thenReturn(List.of(plItem("6001", "主营业务收入", null, "10000")));

        TaxEstimateVo vo = service.estimate(BOOK_ID, "2026-09", URBAN, EDU, LOCAL, CIT, THRESHOLD);

        assertEquals(0, vo.getVatPayable().compareTo(new BigDecimal("-200.00")));
        assertEquals(0, vo.getVatCredit().compareTo(new BigDecimal("200.00")));
        assertEquals(0, vo.getVatDue().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(0, vo.getSurtaxTotal().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(Boolean.TRUE, vo.getBurdenWarning());
    }

    @Test
    void estimate_oldStandardSubjects() {
        stubSubjects();
        // 小企业准则 2171.01.05 销项 / 2171.01.01 进项
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("2221"), any(), any()))
                .thenReturn(List.of());
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("2171"), any(), any()))
                .thenReturn(List.of(
                        taxItem("2171.01.05", "销项税额", null, "260"),
                        taxItem("2171.01.01", "进项税额", "130", null)));
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("5"), any(), any()))
                .thenReturn(List.of(
                        plItem("5001", "主营业务收入", null, "2000"),
                        plItem("5401", "主营业务成本", "1200", null)));
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("6"), any(), any()))
                .thenReturn(List.of());

        TaxEstimateVo vo = service.estimate(BOOK_ID, "2026-09", URBAN, EDU, LOCAL, CIT, THRESHOLD);

        assertEquals(0, vo.getVatDue().compareTo(new BigDecimal("130.00")));
        assertEquals(0, vo.getRevenue().compareTo(new BigDecimal("2000.00")));
        assertEquals(0, vo.getProfitBeforeTax().compareTo(new BigDecimal("800.00")));
    }

    @Test
    void estimate_blankPeriodRejected() {
        assertThrows(BusinessException.class,
                () -> service.estimate(BOOK_ID, " ", URBAN, EDU, LOCAL, CIT, THRESHOLD));
    }

    @Test
    void estimate_badPeriodRejected() {
        assertThrows(BusinessException.class,
                () -> service.estimate(BOOK_ID, "2026/09", URBAN, EDU, LOCAL, CIT, THRESHOLD));
    }

    private void stubSubjects() {
        BookSubject revenue = subject("6001", SubjectDirectionEnum.CREDIT.getValue());
        BookSubject cost = subject("6401", SubjectDirectionEnum.DEBIT.getValue());
        when(bookSubjectMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(revenue, cost));
    }

    private BookSubject subject(String code, String direction) {
        BookSubject s = new BookSubject();
        s.setCode(code);
        s.setName("科目" + code);
        s.setDirection(direction);
        return s;
    }

    private VoucherItemVo taxItem(String code, String name, String debit, String credit) {
        VoucherItemVo item = new VoucherItemVo();
        item.setSubjectCode(code);
        item.setSubjectName(name);
        item.setDebitAmount(debit != null ? new BigDecimal(debit) : null);
        item.setCreditAmount(credit != null ? new BigDecimal(credit) : null);
        return item;
    }

    private VoucherItemVo plItem(String code, String name, String debit, String credit) {
        return taxItem(code, name, debit, credit);
    }
}
