package com.financial.cloud.service.voucher;

import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.voucher.MultiColumnLedgerVo;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiColumnLedgerServiceTest {

    @Mock
    private VoucherItemMapper voucherItemMapper;
    @Mock
    private BookSubjectMapper bookSubjectMapper;

    @InjectMocks
    private MultiColumnLedgerService service;

    private BookSubject subject(String id, String code, String name, String direction, String parentId) {
        BookSubject s = new BookSubject();
        s.setId(id);
        s.setCode(code);
        s.setName(name);
        s.setDirection(direction);
        s.setParentId(parentId);
        s.setBookId("book-1");
        return s;
    }

    private VoucherItemVo item(String voucherId, String date, String word, String summary,
                               String subjectCode, String debit, String credit) throws Exception {
        VoucherItemVo item = new VoucherItemVo();
        item.setVoucherId(voucherId);
        item.setVoucherDate(new SimpleDateFormat("yyyy-MM-dd").parse(date));
        item.setWord(word);
        item.setSummary(summary);
        item.setSubjectCode(subjectCode);
        item.setDebitAmount(debit == null ? null : new BigDecimal(debit));
        item.setCreditAmount(credit == null ? null : new BigDecimal(credit));
        return item;
    }

    @Test
    void directChildCodeExtraction() {
        assertEquals("5602.01", MultiColumnLedgerService.directChildCode("5602", "5602.01"));
        assertEquals("5602.01", MultiColumnLedgerService.directChildCode("5602", "5602.01.02"));
        assertEquals("660201", MultiColumnLedgerService.directChildCode("6602", "660201"));
        assertEquals("660201", MultiColumnLedgerService.directChildCode("6602", "66020103"));
        assertEquals("5602", MultiColumnLedgerService.directChildCode("5602", "5602"));
        assertNull(MultiColumnLedgerService.directChildCode("5602", "5603"));
        assertNull(MultiColumnLedgerService.directChildCode("5602", null));
    }

    @Test
    void queryGroupsByVoucherWithRunningBalance() throws Exception {
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(
                subject("p", "5602", "管理费用", "1", null),
                subject("c1", "5602.01", "办公费", "1", "p"),
                subject("c2", "5602.02", "差旅费", "1", "p")));
        // 期初：起始日前净额 100
        when(voucherItemMapper.multiColumnLedgerItems(eq("book-1"), eq("5602"), isNull(), eq("2026-07-31")))
                .thenReturn(List.of(item("v0", "2026-07-15", "记-1", "上月", "5602.01", "100", null)));
        when(voucherItemMapper.multiColumnLedgerItems(eq("book-1"), eq("5602"), eq("2026-08-01"), eq("2026-08-31")))
                .thenReturn(List.of(
                        item("v1", "2026-08-03", "记-1", "买文具", "5602.01", "200", null),
                        item("v1", "2026-08-03", "记-1", "差旅报销", "5602.02", "300", null),
                        item("v2", "2026-08-10", "记-2", "冲销", "5602.01", null, "50")));

        MultiColumnLedgerVo vo = service.query("book-1", "5602", "2026-08-01", "2026-08-31");

        assertEquals(0, new BigDecimal("100").compareTo(vo.getOpeningBalance()));
        assertEquals(2, vo.getRows().size());
        // v1 行：200 + 300 = 500，余额 600
        MultiColumnLedgerVo.Row r1 = vo.getRows().get(0);
        assertEquals(0, new BigDecimal("500").compareTo(r1.getTotal()));
        assertEquals(0, new BigDecimal("600").compareTo(r1.getBalance()));
        // v2 行：-50，余额 550
        MultiColumnLedgerVo.Row r2 = vo.getRows().get(1);
        assertEquals(0, new BigDecimal("-50").compareTo(r2.getAmounts().get("5602.01")));
        assertEquals(0, new BigDecimal("550").compareTo(r2.getBalance()));
        assertEquals(0, new BigDecimal("550").compareTo(vo.getClosingBalance()));
        assertEquals(0, new BigDecimal("450").compareTo(vo.getPeriodTotal()));
        assertEquals(2, vo.getColumns().size());
    }

    @Test
    void creditNatureParentUsesCreditMinusDebit() throws Exception {
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(
                subject("p", "5001", "主营业务收入", "2", null),
                subject("c1", "5001.01", "产品销售", "2", "p")));
        when(voucherItemMapper.multiColumnLedgerItems(eq("book-1"), eq("5001"), isNull(), eq("2026-07-31")))
                .thenReturn(List.of());
        when(voucherItemMapper.multiColumnLedgerItems(eq("book-1"), eq("5001"), eq("2026-08-01"), eq("2026-08-31")))
                .thenReturn(List.of(
                        item("v1", "2026-08-05", "记-1", "销售", "5001.01", null, "800")));

        MultiColumnLedgerVo vo = service.query("book-1", "5001", "2026-08-01", "2026-08-31");

        assertEquals(0, new BigDecimal("800").compareTo(vo.getPeriodTotal()));
        assertEquals(0, new BigDecimal("800").compareTo(vo.getRows().get(0).getTotal()));
    }

    @Test
    void itemAtParentLevelGetsSelfColumn() throws Exception {
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(
                subject("p", "5602", "管理费用", "1", null),
                subject("c1", "5602.01", "办公费", "1", "p")));
        when(voucherItemMapper.multiColumnLedgerItems(eq("book-1"), eq("5602"), isNull(), eq("2026-07-31")))
                .thenReturn(List.of());
        when(voucherItemMapper.multiColumnLedgerItems(eq("book-1"), eq("5602"), anyString(), anyString()))
                .thenReturn(List.of(
                        item("v1", "2026-08-05", "记-1", "直接记母科目", "5602", "60", null)));

        MultiColumnLedgerVo vo = service.query("book-1", "5602", "2026-08-01", "2026-08-31");

        assertTrue(vo.getColumns().stream().anyMatch(c -> Boolean.TRUE.equals(c.getSelfColumn())));
        assertEquals(0, new BigDecimal("60").compareTo(vo.getPeriodTotal()));
    }

    @Test
    void unknownSubjectRejected() {
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of());
        assertThrows(BusinessException.class,
                () -> service.query("book-1", "9999", "2026-08-01", "2026-08-31"));
    }

    @Test
    void blankSubjectRejected() {
        assertThrows(BusinessException.class,
                () -> service.query("book-1", "  ", "2026-08-01", "2026-08-31"));
    }
}
