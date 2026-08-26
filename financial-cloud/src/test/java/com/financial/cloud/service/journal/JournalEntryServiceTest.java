package com.financial.cloud.service.journal;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.dto.journal.JournalEntryPageDto;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.journal.JournalEntryMapper;
import com.financial.cloud.service.book.SettlementService;
import com.financial.cloud.service.voucher.VoucherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

    @Mock
    private JournalAccountService journalAccountService;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private VoucherService voucherService;
    @Mock
    private SettlementService settlementService;
    @Mock
    private JournalEntryMapper journalEntryMapper;

    @Spy
    @InjectMocks
    private JournalEntryService journalEntryService;

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
}
