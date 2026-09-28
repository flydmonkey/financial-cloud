package com.financial.cloud.service.voucher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.voucher.QuantityLedgerVo;
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
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuantityLedgerServiceTest {

    private static final String BOOK_ID = "book-test-1";

    @Mock
    private VoucherItemMapper voucherItemMapper;
    @Mock
    private BookSubjectMapper bookSubjectMapper;

    @InjectMocks
    private QuantityLedgerService service;

    @Test
    void query_debitSubject_inOutBalance() {
        mockSubject("1403", "库存商品", SubjectDirectionEnum.DEBIT.getValue());
        // 期初：2 件 @10 = 20
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("1403"), isNull(), eq("2026-08-31")))
                .thenReturn(List.of(item("v-open", debit(20), null, 2)));
        // 本期：购入 3 件 @10 = 30；售出 1 件 = 10
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("1403"), eq("2026-09-01"), eq("2026-09-30")))
                .thenReturn(List.of(
                        item("v-1", debit(30), null, 3),
                        item("v-2", null, credit(10), 1)));

        QuantityLedgerVo vo = service.query(BOOK_ID, "1403", "2026-09-01", "2026-09-30");

        assertEquals(2, vo.getOpeningQuantity());
        assertEquals(0, vo.getOpeningAmount().compareTo(new BigDecimal("20")));
        assertEquals(3, vo.getPeriodInQuantity());
        assertEquals(0, vo.getPeriodInAmount().compareTo(new BigDecimal("30")));
        assertEquals(1, vo.getPeriodOutQuantity());
        assertEquals(0, vo.getPeriodOutAmount().compareTo(new BigDecimal("10")));
        assertEquals(4, vo.getClosingQuantity());
        assertEquals(0, vo.getClosingAmount().compareTo(new BigDecimal("40")));

        List<QuantityLedgerVo.Row> rows = vo.getRows();
        assertEquals(2, rows.size());
        assertEquals(3, rows.get(0).getInQuantity());
        assertEquals(0, rows.get(0).getInPrice().compareTo(new BigDecimal("10.00")));
        assertEquals(5, rows.get(0).getBalanceQuantity());
        assertEquals(1, rows.get(1).getOutQuantity());
        assertEquals(4, rows.get(1).getBalanceQuantity());
        assertEquals(0, rows.get(1).getBalancePrice().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void query_creditSubject_incomeOnCreditSide() {
        mockSubject("6001", "主营业务收入", SubjectDirectionEnum.CREDIT.getValue());
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("6001"), isNull(), eq("2026-08-31")))
                .thenReturn(List.of());
        when(voucherItemMapper.multiColumnLedgerItems(eq(BOOK_ID), eq("6001"), eq("2026-09-01"), eq("2026-09-30")))
                .thenReturn(List.of(item("v-1", null, credit(100), 5)));

        QuantityLedgerVo vo = service.query(BOOK_ID, "6001", "2026-09-01", "2026-09-30");

        assertEquals(0, vo.getOpeningQuantity());
        assertEquals(5, vo.getPeriodInQuantity());
        assertEquals(0, vo.getPeriodInAmount().compareTo(new BigDecimal("100")));
        assertEquals(5, vo.getClosingQuantity());
        assertEquals(0, vo.getClosingAmount().compareTo(new BigDecimal("100")));
    }

    @Test
    void query_unknownSubjectRejected() {
        when(bookSubjectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.query(BOOK_ID, "9999", null, null));
    }

    @Test
    void query_blankSubjectRejected() {
        assertThrows(BusinessException.class, () -> service.query(BOOK_ID, " ", null, null));
    }

    @Test
    void unitPrice_zeroQuantityIsNull() {
        assertNull(QuantityLedgerService.unitPrice(new BigDecimal("10"), 0));
        assertEquals(0, QuantityLedgerService.unitPrice(new BigDecimal("10"), 4)
                .compareTo(new BigDecimal("2.50")));
    }

    @Test
    void isIncomeSide_byDirectionAndSide() {
        VoucherItemVo debitItem = item("v", debit(10), null, 1);
        VoucherItemVo creditItem = item("v", null, credit(10), 1);
        // 借方科目：借=收入
        assertEquals(true, QuantityLedgerService.isIncomeSide(debitItem, false));
        assertEquals(false, QuantityLedgerService.isIncomeSide(creditItem, false));
        // 贷方科目：贷=收入
        assertEquals(false, QuantityLedgerService.isIncomeSide(debitItem, true));
        assertEquals(true, QuantityLedgerService.isIncomeSide(creditItem, true));
    }

    private void mockSubject(String code, String name, String direction) {
        BookSubject subject = new BookSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setDirection(direction);
        when(bookSubjectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(subject);
    }

    private static BigDecimal debit(int amount) {
        return new BigDecimal(amount);
    }

    private static BigDecimal credit(int amount) {
        return new BigDecimal(amount);
    }

    private VoucherItemVo item(String voucherId, BigDecimal debit, BigDecimal credit, Integer num) {
        VoucherItemVo item = new VoucherItemVo();
        item.setVoucherId(voucherId);
        item.setVoucherDate(new Date());
        item.setWord("记-1");
        item.setSummary("摘要");
        item.setSubjectCode("1403");
        item.setSubjectName("库存商品");
        item.setDebitAmount(debit);
        item.setCreditAmount(credit);
        item.setNum(num);
        return item;
    }
}
