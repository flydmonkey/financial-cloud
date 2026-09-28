package com.financial.cloud.service.journal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.domain.journal.JournalReconciliation;
import com.financial.cloud.dto.journal.JournalReconciliationDtos;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.journal.JournalEntryMapper;
import com.financial.cloud.repository.journal.JournalReconciliationMapper;
import com.financial.cloud.service.book.BookSealGuard;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalReconciliationServiceTest {

    private static final String BOOK_ID = "book-test-1";
    private static final String ACC_ID = "acc-1";

    @Mock
    private JournalEntryMapper entryMapper;
    @Mock
    private JournalReconciliationMapper reconciliationMapper;
    @Mock
    private JournalAccountService journalAccountService;
    @Mock
    private BookSealGuard bookSealGuard;

    @InjectMocks
    private JournalReconciliationService service;

    @Test
    void get_computesBalanceAndAdjustment() {
        mockAccount();
        // 期初 1000（方向 o，默认已对账）；收入 500 未对账；支出 200 未对账
        when(entryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                entry("e-1", "o", "1000", null, null),
                entry("e-2", "i", "500", null, null),
                entry("e-3", "e", null, "200", null)));
        when(reconciliationMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(JournalReconciliation.builder()
                        .statementBalance(new BigDecimal("1100"))
                        .build());

        JournalReconciliationDtos.ReconciliationVo vo = service.get(BOOK_ID, ACC_ID, "2026-09");

        // 账面余额 = 1000 + 500 - 200 = 1300
        assertEquals(0, vo.getBookBalance().compareTo(new BigDecimal("1300")));
        // 未达：已收未收 500，已付未付 200
        assertEquals(0, vo.getUnreconciledIncome().compareTo(new BigDecimal("500")));
        assertEquals(0, vo.getUnreconciledExpenditure().compareTo(new BigDecimal("200")));
        // 调节后 = 1100 + 500 - 200 = 1400；差额 = 1300 - 1400 = -100
        assertEquals(0, vo.getAdjustedStatement().compareTo(new BigDecimal("1400")));
        assertEquals(0, vo.getDifference().compareTo(new BigDecimal("-100")));
        assertEquals(3, vo.getEntries().size());
        // 期初流水默认已对账
        assertEquals(Boolean.TRUE, vo.getEntries().get(0).getReconciled());
        assertEquals(Boolean.TRUE, vo.getEntries().get(0).getOpening());
        assertEquals(Boolean.FALSE, vo.getEntries().get(1).getReconciled());
    }

    @Test
    void get_balancedWhenAllReconciled() {
        mockAccount();
        when(entryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                entry("e-1", "o", "1000", null, null),
                entry("e-2", "i", "500", null, "y"),
                entry("e-3", "e", null, "200", "y")));
        when(reconciliationMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(JournalReconciliation.builder()
                        .statementBalance(new BigDecimal("1300"))
                        .build());

        JournalReconciliationDtos.ReconciliationVo vo = service.get(BOOK_ID, ACC_ID, "2026-09");

        assertEquals(0, vo.getUnreconciledIncome().compareTo(BigDecimal.ZERO));
        assertEquals(0, vo.getAdjustedStatement().compareTo(new BigDecimal("1300")));
        assertEquals(0, vo.getDifference().compareTo(BigDecimal.ZERO));
    }

    @Test
    void get_noStatement_adjustedIsNull() {
        mockAccount();
        when(entryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(reconciliationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        JournalReconciliationDtos.ReconciliationVo vo = service.get(BOOK_ID, ACC_ID, "2026-09");

        assertEquals(0, vo.getBookBalance().compareTo(BigDecimal.ZERO));
        assertNull(vo.getStatementBalance());
        assertNull(vo.getAdjustedStatement());
        assertNull(vo.getDifference());
    }

    @Test
    void get_foreignAccountRejected() {
        JournalAccount other = new JournalAccount();
        other.setId(ACC_ID);
        other.setBookId("book-other");
        when(journalAccountService.getById(ACC_ID)).thenReturn(other);

        assertThrows(BusinessException.class, () -> service.get(BOOK_ID, ACC_ID, "2026-09"));
    }

    @Test
    void get_badPeriodRejected() {
        mockAccount();
        assertThrows(BusinessException.class, () -> service.get(BOOK_ID, ACC_ID, "2026/09"));
    }

    @Test
    void saveStatement_upserts() {
        mockAccount();
        when(reconciliationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        JournalReconciliationDtos.StatementDto dto = new JournalReconciliationDtos.StatementDto();
        dto.setAccId(ACC_ID);
        dto.setYearPeriod("2026-09");
        dto.setStatementBalance(new BigDecimal("999"));

        JournalReconciliation saved = service.saveStatement(BOOK_ID, dto);

        assertEquals(BOOK_ID, saved.getBookId());
        assertEquals(ACC_ID, saved.getAccId());
        assertEquals("2026-09", saved.getYearPeriod());
    }

    @Test
    void mark_foreignEntryRejected() {
        JournalEntry foreign = entry("e-9", "i", "1", null, null);
        foreign.setBookId("book-other");
        when(entryMapper.selectById("e-9")).thenReturn(foreign);

        JournalReconciliationDtos.MarkDto dto = new JournalReconciliationDtos.MarkDto();
        dto.setEntryIds(List.of("e-9"));
        dto.setReconciled(true);

        assertThrows(BusinessException.class, () -> service.mark(BOOK_ID, dto));
    }

    private void mockAccount() {
        JournalAccount account = new JournalAccount();
        account.setId(ACC_ID);
        account.setBookId(BOOK_ID);
        account.setAccName("基本户");
        when(journalAccountService.getById(ACC_ID)).thenReturn(account);
    }

    private JournalEntry entry(String id, String direction, String income, String expenditure, String reconciled) {
        JournalEntry entry = new JournalEntry();
        entry.setId(id);
        entry.setBookId(BOOK_ID);
        entry.setAccId(ACC_ID);
        entry.setDirection(direction);
        entry.setIncome(income != null ? new BigDecimal(income) : null);
        entry.setExpenditure(expenditure != null ? new BigDecimal(expenditure) : null);
        entry.setTradeDate(new Date());
        entry.setReconciled(reconciled);
        return entry;
    }
}
