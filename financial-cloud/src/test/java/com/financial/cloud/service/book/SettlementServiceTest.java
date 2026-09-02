package com.financial.cloud.service.book;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.dto.book.SettlementPageDto;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.SettlementMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.journal.JournalAccountService;
import com.financial.cloud.service.statement.StatementBalanceSheetService;
import com.financial.cloud.service.statement.StatementIncomeService;
import com.financial.cloud.service.statement.StatementReportService;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;
import com.financial.cloud.service.voucher.VoucherService;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.repository.journal.JournalEntryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final String BOOK_ID = "book-test-1";
    private static final String USER_ID = "user-1";

    @Mock
    private IdentifierGenerator identifierGenerator;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookSubjectService bookSubjectService;
    @Mock
    private SettlementMapper settlementMapper;
    @Mock
    private ConfigSysService configSysService;
    @Mock
    private VoucherService voucherService;
    @Mock
    private VoucherItemMapper voucherItemMapper;
    @Mock
    private StatementIncomeService statementIncomeService;
    @Mock
    private StatementBalanceSheetService statementBalanceSheetService;
    @Mock
    private StatementSubjectBalanceService statementSubjectBalanceService;
    @Mock
    private JournalAccountService journalAccountService;
    @Mock
    private JournalEntryMapper journalEntryMapper;
    @Mock
    private StatementReportService statementReportService;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    void pageList_defaultsYearFromCurrentTermWhenMissing() {
        SettlementPageDto dto = new SettlementPageDto();
        dto.setBookId(BOOK_ID);
        dto.setYear(0);

        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-03");
        when(configSysService.getTermStart(BOOK_ID)).thenReturn("2025-01");
        when(settlementMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());

        Message<Page<Settlement>> result = settlementService.pageList(dto);

        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals(2025, dto.getYear());
        assertEquals(12, result.getData().getRecords().size());
    }

    @Test
    void checkout_rejectsWhenCurrentTermAlreadySettled() {
        Settlement dto = new Settlement();
        dto.setBookId(BOOK_ID);
        dto.setYear(2025);

        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-03");
        when(settlementMapper.selectOne(any())).thenReturn(new Settlement());

        Message<Settlement> result = settlementService.checkout(dto);

        assertEquals(Message.FAIL, result.getCode());
        assertTrue(result.getMessage().contains("已结账"));
        verify(configSysService, never()).termToNext(BOOK_ID);
    }

    @Test
    void uncheckout_rejectsNonAdjacentPeriod() {
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-03");

        Message<String> result = settlementService.uncheckout(BOOK_ID, "2025-01", USER_ID);

        assertEquals(Message.FAIL, result.getCode());
        assertTrue(result.getMessage().contains("只能反结账最近已结期间"));
        verify(settlementMapper, never()).deleteById(anyString());
        verify(configSysService, never()).updateCurrentTerm(anyString(), anyString());
    }

    @Test
    void uncheckout_rejectsWhenNewerPeriodHasVouchers() {
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-03");
        Settlement closed = new Settlement();
        closed.setId("s-2025-02");
        when(settlementMapper.selectOne(any())).thenReturn(closed);
        when(voucherService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        Message<String> result = settlementService.uncheckout(BOOK_ID, "2025-02", USER_ID);

        assertEquals(Message.FAIL, result.getCode());
        assertTrue(result.getMessage().contains("已有凭证"));
        verify(settlementMapper, never()).deleteById(anyString());
    }

    @Test
    void uncheckout_rejectsWhenJournalSnapshotMissing() {
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-03");
        Settlement closed = new Settlement();
        closed.setId("s-2025-02");
        when(settlementMapper.selectOne(any())).thenReturn(closed);
        when(voucherService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(journalEntryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(journalAccountService.hasAccountsMissingPrevOpening(BOOK_ID)).thenReturn(true);

        Message<String> result = settlementService.uncheckout(BOOK_ID, null, USER_ID);

        assertEquals(Message.FAIL, result.getCode());
        assertTrue(result.getMessage().contains("未保存日记账期初快照"));
        verify(settlementMapper, never()).deleteById(anyString());
    }

    @Test
    void uncheckout_successReopensPreviousMonth() {
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-03");
        Settlement closed = new Settlement();
        closed.setId("s-2025-02");
        when(settlementMapper.selectOne(any())).thenReturn(closed);
        when(voucherService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(journalEntryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(journalAccountService.hasAccountsMissingPrevOpening(BOOK_ID)).thenReturn(false);

        Message<String> result = settlementService.uncheckout(BOOK_ID, "2025-02", USER_ID);

        assertEquals(Message.SUCCESS, result.getCode());
        verify(statementSubjectBalanceService).deleteByBookAndPeriod(eq(BOOK_ID), eq("2025-03"), eq("month"));
        verify(journalAccountService).restoreOpeningFromPrev(BOOK_ID);
        verify(statementIncomeService).deletePeriodSnapshot(eq(BOOK_ID), eq("2025-02"), eq("month"));
        verify(statementBalanceSheetService).deletePeriodSnapshot(eq(BOOK_ID), eq("2025-02"), eq("month"));
        verify(settlementMapper).deleteById("s-2025-02");
        verify(configSysService).updateCurrentTerm(BOOK_ID, "2025-02");
    }
}
