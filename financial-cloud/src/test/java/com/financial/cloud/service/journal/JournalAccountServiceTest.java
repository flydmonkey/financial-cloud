package com.financial.cloud.service.journal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.repository.journal.JournalAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalAccountServiceTest {

    @Mock
    private JournalAccountMapper journalAccountMapper;

    @InjectMocks
    private JournalAccountService journalAccountService;

    @BeforeEach
    void wireBaseMapper() {
        ReflectionTestUtils.setField(journalAccountService, "baseMapper", journalAccountMapper);
    }

    @Test
    void checkout_delegatesToMapperSnapshotSql() {
        when(journalAccountMapper.checkout("book-1")).thenReturn(2);
        assertTrue(journalAccountService.checkout("book-1") == 2);
        verify(journalAccountMapper).checkout("book-1");
    }

    @Test
    void hasAccountsMissingPrevOpening_trueWhenAnyNull() {
        JournalAccount a = JournalAccount.builder()
                .bookId("book-1")
                .openingBalance(BigDecimal.TEN)
                .prevOpeningBalance(null)
                .build();
        when(journalAccountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
        assertTrue(journalAccountService.hasAccountsMissingPrevOpening("book-1"));
    }

    @Test
    void hasAccountsMissingPrevOpening_falseWhenEmptyOrAllPresent() {
        when(journalAccountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertFalse(journalAccountService.hasAccountsMissingPrevOpening("book-1"));

        JournalAccount a = JournalAccount.builder()
                .bookId("book-1")
                .prevOpeningBalance(BigDecimal.ZERO)
                .build();
        when(journalAccountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
        assertFalse(journalAccountService.hasAccountsMissingPrevOpening("book-1"));
    }
}
