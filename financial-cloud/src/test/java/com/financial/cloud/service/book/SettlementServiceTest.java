package com.financial.cloud.service.book;

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
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.dto.book.SettlementPageDto;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.book.BookSubjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final String BOOK_ID = "book-test-1";

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
}
