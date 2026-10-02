package com.financial.cloud.service.book;

import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementCarryServiceTest {
    private VoucherItemChangeDto item(String debit, String credit) {
        VoucherItemChangeDto item = new VoucherItemChangeDto();
        item.setDebitAmount(new BigDecimal(debit));
        item.setCreditAmount(new BigDecimal(credit));
        return item;
    }

    @Test
    void negativeExpenseClosesWithDebit() {
        VoucherItemChangeDto result = SettlementCarryService.signedCarryItem(item("0", "200"), new BigDecimal("-200"));
        assertEquals(new BigDecimal("200"), result.getDebitAmount());
        assertEquals(BigDecimal.ZERO, result.getCreditAmount());
    }

    @Test
    void reversedIncomeDebitBalanceClosesWithCredit() {
        VoucherItemChangeDto result = SettlementCarryService.signedCarryItem(item("150", "0"), new BigDecimal("150"));
        assertEquals(BigDecimal.ZERO, result.getDebitAmount());
        assertEquals(new BigDecimal("150"), result.getCreditAmount());
    }

    @Test
    void normalIncomeCreditBalanceClosesWithDebit() {
        VoucherItemChangeDto result = SettlementCarryService.signedCarryItem(item("150", "0"), new BigDecimal("-150"));
        assertEquals(new BigDecimal("150"), result.getDebitAmount());
        assertEquals(BigDecimal.ZERO, result.getCreditAmount());
    }

    @Test
    void mixedExpenseBalancesUseSignedNetForCounterpart() {
        VoucherItemChangeDto normal = SettlementCarryService.signedCarryItem(item("0", "100"), new BigDecimal("100"));
        VoucherItemChangeDto reversal = SettlementCarryService.signedCarryItem(item("0", "200"), new BigDecimal("-200"));
        assertEquals(new BigDecimal("100"), SettlementCarryService.carryNetDebit(List.of(normal, reversal)));
    }

    @Test
    void offsettingBalancesNeedNoProfitCounterpart() {
        assertEquals(BigDecimal.ZERO, SettlementCarryService.carryNetDebit(List.of(item("100", "0"), item("0", "100"))));
    }
}
