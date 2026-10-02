package com.financial.cloud.service.journal;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.journal.JournalEntryDto;
import com.financial.cloud.dto.journal.JournalEntryPageDto;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.enums.error.JournalErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.journal.JournalEntryMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.book.SettlementService;
import com.financial.cloud.service.voucher.VoucherService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

    @Mock
    private JournalAccountService journalAccountService;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookSubjectService bookSubjectService;
    @Mock
    private VoucherService voucherService;
    @Mock
    private SettlementService settlementService;
    @Mock
    private JournalEntryMapper journalEntryMapper;

    @Spy
    @InjectMocks
    private JournalEntryService journalEntryService;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                JournalEntry.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                JournalAccount.class);
    }

    @Test
    void pageList_returnsPagedEntries() {
        JournalEntryPageDto dto = new JournalEntryPageDto();
        dto.setPageNumber(1);
        dto.setPageSize(10);

        Page<JournalEntry> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(new JournalEntry()));
        page.setTotal(1);

        doReturn(journalEntryMapper).when(journalEntryService).getBaseMapper();
        when(journalEntryMapper.pageList(any(), any())).thenReturn(page);

        Message<Page<JournalEntry>> result = journalEntryService.pageList(dto);

        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void update_rejectsMoneyFieldChangeWhenVoucherLinked() {
        JournalEntry existing = baseEntry("e1", "acc1", "i", "10", null);
        existing.setVoucherId("v1");
        existing.setSubjectId("sub-counter");

        when(settlementService.check(anyString(), anyString())).thenReturn(Message.ok("ok"));
        doReturn(existing).when(journalEntryService).getById("e1");

        JournalEntryDto dto = new JournalEntryDto();
        dto.setId("e1");
        dto.setBookId("book1");
        dto.setAccId("acc1");
        dto.setDirection("i");
        dto.setIncome(new BigDecimal("20"));
        dto.setSubjectId("sub-counter");
        dto.setTradeDate(existing.getTradeDate());

        BusinessException ex = assertThrows(BusinessException.class, () -> journalEntryService.update(dto));
        assertEquals(JournalErrorCode.ENTRY_LINKED_LOCKED.getCode(), ex.getCode());
        verify(journalEntryService, never()).updateById(any());
    }

    @Test
    void update_recalculatesLaterRunningBalancesOnAmountChange() {
        Date day1 = new Date(1_700_000_000_000L);
        Date day2 = new Date(1_700_086_400_000L);
        JournalEntry first = baseEntry("e1", "acc1", "i", "50", null);
        first.setTradeDate(day1);
        first.setBalance(new BigDecimal("50"));
        JournalEntry second = baseEntry("e2", "acc1", "e", null, "20");
        second.setTradeDate(day2);
        second.setBalance(new BigDecimal("30"));

        when(settlementService.check(anyString(), anyString())).thenReturn(Message.ok("ok"));
        doReturn(first).when(journalEntryService).getById("e1");
        doAnswer(invocation -> {
            JournalEntry updated = invocation.getArgument(0);
            if ("e1".equals(updated.getId())) {
                first.setIncome(updated.getIncome());
                first.setExpenditure(updated.getExpenditure());
                first.setDirection(updated.getDirection());
                first.setBalance(updated.getBalance());
            } else if ("e2".equals(updated.getId())) {
                second.setBalance(updated.getBalance());
            }
            return true;
        }).when(journalEntryService).updateById(any(JournalEntry.class));
        doReturn(List.of(first, second)).when(journalEntryService).list(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());
        when(journalAccountService.setBalance(eq("acc1"), any())).thenReturn(true);

        JournalEntryDto dto = new JournalEntryDto();
        dto.setId("e1");
        dto.setBookId("book1");
        dto.setAccId("acc1");
        dto.setDirection("i");
        dto.setIncome(new BigDecimal("80"));
        dto.setSubjectId("sub-counter");
        dto.setTradeDate(day1);

        Message<String> result = journalEntryService.update(dto);

        assertEquals(Message.SUCCESS, result.getCode());
        ArgumentCaptor<BigDecimal> balanceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(journalAccountService).setBalance(eq("acc1"), balanceCaptor.capture());
        assertEquals(0, new BigDecimal("60").compareTo(balanceCaptor.getValue()));
    }

    @Test
    void generateVoucher_incomeMapsFundDebitAndCounterpartCredit() {
        JournalEntry entry = baseEntry("e1", "acc1", "i", "100", null);
        entry.setSubjectId("sub-counter");
        entry.setTradeDate(new Date(1_700_000_000_000L));
        entry.setRemark("收款");

        JournalAccount account = new JournalAccount();
        account.setId("acc1");
        account.setSubjectId("sub-fund");

        BookSubject fund = subject("sub-fund", "1001", "库存现金");
        BookSubject counter = subject("sub-counter", "5001", "管理费用");
        Book book = new Book();
        book.setCompanyName("Demo Co");

        when(settlementService.check(anyString(), anyString())).thenReturn(Message.ok("ok"));
        doReturn(entry).when(journalEntryService).getById("e1");
        when(bookMapper.selectById("book1")).thenReturn(book);
        when(journalAccountService.getById("acc1")).thenReturn(account);
        when(bookSubjectService.getById("sub-fund")).thenReturn(fund);
        when(bookSubjectService.getById("sub-counter")).thenReturn(counter);
        when(voucherService.getAbleWordNum(eq("book1"), eq("记"), any(), any())).thenReturn(Message.ok(1));
        when(voucherService.save(any(VoucherChangeDto.class))).thenAnswer(invocation -> {
            VoucherChangeDto voucher = invocation.getArgument(0);
            voucher.setId("v-new");
            return Message.ok("v-new");
        });
        doReturn(true).when(journalEntryService).update(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());

        GenerateVoucherDto dto = new GenerateVoucherDto();
        dto.setId("e1");
        dto.setBookId("book1");

        Message<String> result = journalEntryService.generateVoucher(dto);

        assertEquals(Message.SUCCESS, result.getCode());
        ArgumentCaptor<VoucherChangeDto> voucherCaptor = ArgumentCaptor.forClass(VoucherChangeDto.class);
        verify(voucherService).save(voucherCaptor.capture());
        VoucherChangeDto saved = voucherCaptor.getValue();
        assertEquals(entry.getTradeDate(), saved.getVoucherDate());
        assertEquals("sub-fund", saved.getItems().get(0).getSubjectId());
        assertEquals("1001-库存现金", saved.getItems().get(0).getSubjectName());
        assertEquals(0, new BigDecimal("100").compareTo(saved.getItems().get(0).getDebitAmount()));
        assertEquals("sub-counter", saved.getItems().get(1).getSubjectId());
        assertEquals(0, new BigDecimal("100").compareTo(saved.getItems().get(1).getCreditAmount()));
    }

    @Test
    void generateVoucher_expenditureMapsCounterpartDebitAndFundCredit() {
        JournalEntry entry = baseEntry("e1", "acc1", "e", null, "40");
        entry.setSubjectId("sub-counter");
        entry.setTradeDate(new Date(1_700_000_000_000L));

        JournalAccount account = new JournalAccount();
        account.setId("acc1");
        account.setSubjectId("sub-fund");

        when(settlementService.check(anyString(), anyString())).thenReturn(Message.ok("ok"));
        doReturn(entry).when(journalEntryService).getById("e1");
        when(bookMapper.selectById("book1")).thenReturn(new Book());
        when(journalAccountService.getById("acc1")).thenReturn(account);
        when(bookSubjectService.getById("sub-fund")).thenReturn(subject("sub-fund", "1002", "银行存款"));
        when(bookSubjectService.getById("sub-counter")).thenReturn(subject("sub-counter", "5602", "管理费用"));
        when(voucherService.getAbleWordNum(anyString(), anyString(), any(), any())).thenReturn(Message.ok(2));
        when(voucherService.save(any(VoucherChangeDto.class))).thenAnswer(invocation -> {
            VoucherChangeDto voucher = invocation.getArgument(0);
            voucher.setId("v2");
            return Message.ok("v2");
        });
        doReturn(true).when(journalEntryService).update(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());

        GenerateVoucherDto dto = new GenerateVoucherDto();
        dto.setId("e1");
        dto.setBookId("book1");

        journalEntryService.generateVoucher(dto);

        ArgumentCaptor<VoucherChangeDto> voucherCaptor = ArgumentCaptor.forClass(VoucherChangeDto.class);
        verify(voucherService).save(voucherCaptor.capture());
        assertEquals("sub-counter", voucherCaptor.getValue().getItems().get(0).getSubjectId());
        assertEquals("sub-fund", voucherCaptor.getValue().getItems().get(1).getSubjectId());
    }

    @Test
    void generateVoucher_rejectsOpeningInit() {
        JournalEntry entry = baseEntry("e1", "acc1", "o", "10", null);
        doReturn(entry).when(journalEntryService).getById("e1");
        when(bookMapper.selectById("book1")).thenReturn(new Book());

        GenerateVoucherDto dto = new GenerateVoucherDto();
        dto.setId("e1");
        dto.setBookId("book1");

        BusinessException ex = assertThrows(BusinessException.class, () -> journalEntryService.generateVoucher(dto));
        assertEquals(JournalErrorCode.OPENING_CANNOT_GENERATE.getCode(), ex.getCode());
    }

    @Test
    void generateVoucher_rejectsDuplicateLink() {
        JournalEntry entry = baseEntry("e1", "acc1", "i", "10", null);
        entry.setVoucherId("v-existing");
        doReturn(entry).when(journalEntryService).getById("e1");
        when(bookMapper.selectById("book1")).thenReturn(new Book());

        GenerateVoucherDto dto = new GenerateVoucherDto();
        dto.setId("e1");
        dto.setBookId("book1");

        BusinessException ex = assertThrows(BusinessException.class, () -> journalEntryService.generateVoucher(dto));
        assertEquals(JournalErrorCode.VOUCHER_ALREADY_LINKED.getCode(), ex.getCode());
    }

    @Test
    void generateVoucher_rejectsMissingSubject() {
        JournalEntry entry = baseEntry("e1", "acc1", "i", "10", null);
        entry.setSubjectId(null);
        entry.setTradeDate(new Date());
        JournalAccount account = new JournalAccount();
        account.setId("acc1");
        account.setSubjectId(null);

        when(settlementService.check(anyString(), anyString())).thenReturn(Message.ok("ok"));
        doReturn(entry).when(journalEntryService).getById("e1");
        when(bookMapper.selectById("book1")).thenReturn(new Book());
        when(journalAccountService.getById("acc1")).thenReturn(account);

        GenerateVoucherDto dto = new GenerateVoucherDto();
        dto.setId("e1");
        dto.setBookId("book1");

        BusinessException ex = assertThrows(BusinessException.class, () -> journalEntryService.generateVoucher(dto));
        assertEquals(JournalErrorCode.SUBJECT_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void generateVoucher_rejectsSameFundAndCounterpartSubject() {
        JournalEntry entry = baseEntry("e1", "acc1", "i", "10", null);
        entry.setSubjectId("sub-fund");
        entry.setTradeDate(new Date());
        JournalAccount account = new JournalAccount();
        account.setId("acc1");
        account.setSubjectId("sub-fund");

        when(settlementService.check(anyString(), anyString())).thenReturn(Message.ok("ok"));
        doReturn(entry).when(journalEntryService).getById("e1");
        when(bookMapper.selectById("book1")).thenReturn(new Book());
        when(journalAccountService.getById("acc1")).thenReturn(account);

        GenerateVoucherDto dto = new GenerateVoucherDto();
        dto.setId("e1");
        dto.setBookId("book1");

        BusinessException ex = assertThrows(BusinessException.class, () -> journalEntryService.generateVoucher(dto));
        assertEquals(JournalErrorCode.SUBJECT_SAME_AS_FUND.getCode(), ex.getCode());
    }

    @Test
    void delete_reportsPartialSkipForClosedPeriod() {
        JournalEntry open = baseEntry("e1", "acc1", "i", "10", null);
        open.setTradeDate(new Date());
        JournalEntry closed = baseEntry("e2", "acc1", "i", "5", null);
        closed.setTradeDate(new Date());

        doReturn(open).when(journalEntryService).getById("e1");
        doReturn(closed).when(journalEntryService).getById("e2");
        when(settlementService.check(eq("book1"), anyString()))
                .thenReturn(Message.ok("ok"))
                .thenReturn(Message.failed("已结账"));
        doReturn(true).when(journalEntryService).removeBatchByIds(any());
        doReturn(List.of()).when(journalEntryService).list(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());
        when(journalAccountService.setBalance(eq("acc1"), any())).thenReturn(true);

        ListIdsDto dto = new ListIdsDto();
        dto.setListIds(List.of("e1", "e2"));

        Message<String> result = journalEntryService.delete(dto);

        assertEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("跳过"));
        verify(journalEntryService).removeBatchByIds(List.of("e1"));
    }

    @Test
    void createReversalEntriesForVoucher_flipsIncomeToExpenditure() {
        JournalEntry linked = baseEntry("e1", "acc1", "i", "100", null);
        linked.setVoucherId("v-src");
        linked.setRemark("银行收款");
        final int[] listCalls = {0};
        doAnswer(inv -> {
            listCalls[0]++;
            // 1st list: source links; subsequent lists: balance rebuild (include new reverse via save side-effect)
            return listCalls[0] == 1 ? List.of(linked) : List.of();
        }).when(journalEntryService).list(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());
        doReturn(true).when(journalEntryService).save(any(JournalEntry.class));
        when(journalAccountService.setBalance(eq("acc1"), any())).thenReturn(true);

        int n = journalEntryService.createReversalEntriesForVoucher(
                "v-src", "v-rev", "book1", new Date());
        assertEquals(1, n);
        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryService).save(captor.capture());
        JournalEntry rev = captor.getValue();
        assertEquals("v-rev", rev.getVoucherId());
        assertEquals("e", rev.getDirection());
        assertEquals(0, new BigDecimal("100").compareTo(rev.getExpenditure()));
        assertTrue(rev.getRemark().contains("冲销"));
    }

    @Test
    void clearLinksByVoucherIds_updatesNullVoucherId() {
        doReturn(true).when(journalEntryService).update(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());
        journalEntryService.clearLinksByVoucherIds(List.of("v-1"));
        verify(journalEntryService).update(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());
    }

    @Test
    void syncLinkedEntriesFromVoucher_writesFundDebitAsIncome() {
        JournalEntry linked = baseEntry("e1", "acc1", "i", "100", null);
        linked.setVoucherId("v-1");
        JournalAccount account = new JournalAccount();
        account.setId("acc1");
        account.setSubjectId("fund-sub");
        when(journalAccountService.getById("acc1")).thenReturn(account);

        doAnswer(inv -> List.of(linked)).when(journalEntryService)
                .list(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());
        doReturn(true).when(journalEntryService).updateById(any(JournalEntry.class));
        when(journalAccountService.setBalance(eq("acc1"), any())).thenReturn(true);

        com.financial.cloud.domain.voucher.VoucherItem fund = new com.financial.cloud.domain.voucher.VoucherItem();
        fund.setSubjectId("fund-sub");
        fund.setDebitAmount(new BigDecimal("250"));
        com.financial.cloud.domain.voucher.VoucherItem counter = new com.financial.cloud.domain.voucher.VoucherItem();
        counter.setSubjectId("sub-counter-2");
        counter.setCreditAmount(new BigDecimal("250"));

        journalEntryService.syncLinkedEntriesFromVoucher(
                "v-1", "book1", new Date(), "回写备注", List.of(fund, counter));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryService, org.mockito.Mockito.atLeastOnce()).updateById(captor.capture());
        boolean synced = captor.getAllValues().stream().anyMatch(e ->
                e.getIncome() != null && e.getIncome().compareTo(new BigDecimal("250")) == 0
                        && "回写备注".equals(e.getRemark())
                        && "sub-counter-2".equals(e.getSubjectId()));
        assertTrue(synced);
    }

    @Test
    void syncLinkedEntriesFromVoucher_mapsNegativeDebitToExpense() {
        JournalEntry linked = baseEntry("e1", "acc1", "i", "100", null);
        linked.setVoucherId("v-rev");
        JournalEntry opening = baseEntry("e0", "acc1", "o", "500", null);
        JournalAccount account = new JournalAccount();
        account.setId("acc1");
        account.setSubjectId("fund-sub");
        when(journalAccountService.getById("acc1")).thenReturn(account);

        // 1st list: linked for sync; later lists: balance rebuild (need opening cover expense)
        doAnswer(inv -> List.of(linked))
                .doAnswer(inv -> List.of(opening, linked))
                .when(journalEntryService)
                .list(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());
        doReturn(true).when(journalEntryService).updateById(any(JournalEntry.class));
        when(journalAccountService.setBalance(eq("acc1"), any())).thenReturn(true);

        com.financial.cloud.domain.voucher.VoucherItem fund = new com.financial.cloud.domain.voucher.VoucherItem();
        fund.setSubjectId("fund-sub");
        fund.setDebitAmount(new BigDecimal("-80"));
        com.financial.cloud.domain.voucher.VoucherItem counter = new com.financial.cloud.domain.voucher.VoucherItem();
        counter.setSubjectId("sub-counter-2");
        counter.setCreditAmount(new BigDecimal("-80"));

        journalEntryService.syncLinkedEntriesFromVoucher(
                "v-rev", "book1", new Date(), "冲销回写", List.of(fund, counter));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryService, org.mockito.Mockito.atLeastOnce()).updateById(captor.capture());
        boolean synced = captor.getAllValues().stream().anyMatch(e ->
                "e".equalsIgnoreCase(e.getDirection())
                        && e.getExpenditure() != null
                        && e.getExpenditure().compareTo(new BigDecimal("80")) == 0);
        assertTrue(synced);
    }

    @Test
    void syncLinkedEntriesFromVoucher_rejectsMissingFundLine() {
        JournalEntry linked = baseEntry("e1", "acc1", "i", "100", null);
        linked.setVoucherId("v-1");
        doReturn(List.of(linked)).when(journalEntryService)
                .list(org.mockito.ArgumentMatchers.<Wrapper<JournalEntry>>any());
        JournalAccount account = new JournalAccount();
        account.setId("acc1");
        account.setSubjectId("fund-sub");
        when(journalAccountService.getById("acc1")).thenReturn(account);

        com.financial.cloud.domain.voucher.VoucherItem onlyCounter = new com.financial.cloud.domain.voucher.VoucherItem();
        onlyCounter.setSubjectId("other");
        onlyCounter.setDebitAmount(new BigDecimal("100"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                journalEntryService.syncLinkedEntriesFromVoucher(
                        "v-1", "book1", new Date(), "x", List.of(onlyCounter)));
        assertEquals(JournalErrorCode.VOUCHER_SYNC_STRUCTURE.getCode(), ex.getCode());
    }

    private static JournalEntry baseEntry(String id, String accId, String direction, String income, String expenditure) {
        JournalEntry entry = new JournalEntry();
        entry.setId(id);
        entry.setBookId("book1");
        entry.setAccId(accId);
        entry.setDirection(direction);
        entry.setIncome(income == null ? null : new BigDecimal(income));
        entry.setExpenditure(expenditure == null ? null : new BigDecimal(expenditure));
        entry.setTradeDate(new Date());
        entry.setSubjectId("sub-counter");
        return entry;
    }

    private static BookSubject subject(String id, String code, String name) {
        BookSubject subject = new BookSubject();
        subject.setId(id);
        subject.setCode(code);
        subject.setName(name);
        return subject;
    }
}
