package com.financial.cloud.service.statement;

import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementGeneralLedgerItem;
import com.financial.cloud.dto.statement.StatementGeneralLedgerReport;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.enums.common.YesNoEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.util.StatementGeneralLedgerRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementGeneralLedgerServiceTest {

    @Mock
    private StatementSubjectBalanceMapper subjectBalanceMapper;
    @Mock
    private BookSubjectMapper bookSubjectMapper;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private VoucherItemMapper voucherItemMapper;

    @InjectMocks
    private StatementGeneralLedgerService service;

    @Test
    void query_defaultLevel1_usesPostedVoucherForPeriod() {
        StatementParamsDto dto = StatementParamsDto.builder()
                .bookId("book-1")
                .periodType("between")
                .dateRange(new String[]{"2026-08", "2026-08"})
                .hideNoActivityAndZeroBalance(false)
                .build();

        BookSubject cash = subject("s1", null, "1002", "银行存款", 1, SubjectDirectionEnum.DEBIT.getValue());
        BookSubject capital = subject("s2", null, "3103", "本年利润", 1, SubjectDirectionEnum.CREDIT.getValue());
        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(cash, capital));
        when(subjectBalanceMapper.selectList(any())).thenReturn(List.of(
                bal("1002", "2026-08", "0", "0"),
                bal("3103", "2026-08", "0", "0")
        ));
        // period + ytd 两次查询
        when(voucherItemMapper.selectSubjectAmount(any())).thenReturn(
                List.of(voucher("1002", "100", "0"), voucher("3103", "0", "100")),
                List.of(voucher("1002", "100", "0"), voucher("3103", "0", "100"))
        );

        Message<StatementGeneralLedgerReport> message = service.query(dto);
        assertEquals(Message.SUCCESS, message.getCode());
        StatementGeneralLedgerReport report = message.getData();
        assertEquals(1, dto.getMaxLevel().intValue());
        assertEquals(2, report.getSubjectCount());
        assertTrue(Boolean.TRUE.equals(report.getTrialBalanced()));
        assertEquals(0, new BigDecimal("100").compareTo(report.getPeriodDebitTotal()));
        assertEquals(0, new BigDecimal("100").compareTo(report.getPeriodCreditTotal()));

        StatementGeneralLedgerItem periodCash = report.getItems().stream()
                .filter(i -> "1002".equals(i.getSubjectCode())
                        && StatementGeneralLedgerRules.SUMMARY_PERIOD.equals(i.getSummary()))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("100").compareTo(periodCash.getDebit()));
        assertNull(periodCash.getCredit());
    }

    @Test
    void query_hidesNoActivityZeroByDefault() {
        StatementParamsDto dto = StatementParamsDto.builder()
                .bookId("book-1")
                .periodType("between")
                .dateRange(new String[]{"2026-08", "2026-08"})
                .build();

        when(bookSubjectMapper.selectList(any())).thenReturn(List.of(
                subject("s1", null, "1001", "库存现金", 1, SubjectDirectionEnum.DEBIT.getValue())));
        when(subjectBalanceMapper.selectList(any())).thenReturn(List.of(
                bal("1001", "2026-08", "0", "0")));
        when(voucherItemMapper.selectSubjectAmount(any())).thenReturn(List.of(), List.of());

        StatementGeneralLedgerReport report = service.query(dto).getData();
        assertEquals(0, report.getSubjectCount());
        assertEquals(0, report.getItems().size());
    }

    private static BookSubject subject(String id, String parentId, String code, String name,
                                       int level, String direction) {
        BookSubject s = new BookSubject();
        s.setId(id);
        s.setParentId(parentId);
        s.setCode(code);
        s.setName(name);
        s.setLevel(level);
        s.setDirection(direction);
        return s;
    }

    private static StatementSubjectBalance bal(String code, String period, String openD, String openC) {
        return StatementSubjectBalance.builder()
                .subjectCode(code)
                .yearPeriod(period)
                .direction(SubjectDirectionEnum.DEBIT.getValue())
                .isAuxiliary(YesNoEnum.n.name())
                .openingBalanceDebit(new BigDecimal(openD))
                .openingBalanceCredit(new BigDecimal(openC))
                .currentPeriodDebit(BigDecimal.ZERO)
                .currentPeriodCredit(BigDecimal.ZERO)
                .yearToDateDebit(BigDecimal.ZERO)
                .yearToDateCredit(BigDecimal.ZERO)
                .closingBalanceDebit(BigDecimal.ZERO)
                .closingBalanceCredit(BigDecimal.ZERO)
                .build();
    }

    private static VoucherItemVo voucher(String code, String debit, String credit) {
        VoucherItemVo vo = new VoucherItemVo();
        vo.setSubjectCode(code);
        vo.setDebitAmount(new BigDecimal(debit));
        vo.setCreditAmount(new BigDecimal(credit));
        return vo;
    }
}
