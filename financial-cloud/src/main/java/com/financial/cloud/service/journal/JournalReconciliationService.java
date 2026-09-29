package com.financial.cloud.service.journal;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.domain.journal.JournalReconciliation;
import com.financial.cloud.dto.journal.JournalReconciliationDtos;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.journal.JournalEntryMapper;
import com.financial.cloud.repository.journal.JournalReconciliationMapper;
import com.financial.cloud.service.book.BookSealGuard;
import com.financial.cloud.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 银行对账 / 余额调节表：
 * 企业日记账余额（流水净额累计）vs 银行对账单余额（按账户+期间登记），
 * 未对账流水构成未达账项（企业已收银行未收 / 企业已付银行未付），
 * 调节后银行余额应与账面余额一致。期初流水（方向 o）默认视为已对账。
 */
@Service
@RequiredArgsConstructor
public class JournalReconciliationService {

    private final JournalEntryMapper entryMapper;
    private final JournalReconciliationMapper reconciliationMapper;
    private final JournalAccountService journalAccountService;
    private final BookSealGuard bookSealGuard;

    public JournalReconciliationDtos.ReconciliationVo get(String bookId, String accId, String yearPeriod) {
        JournalAccount account = requireAccount(bookId, accId);
        YearMonth ym = parsePeriod(yearPeriod);
        Date periodEnd = endOfMonth(ym);

        List<JournalEntry> entries = entryMapper.selectList(Wrappers.<JournalEntry>lambdaQuery()
                .eq(JournalEntry::getBookId, bookId)
                .eq(JournalEntry::getAccId, accId)
                .le(JournalEntry::getTradeDate, periodEnd)
                .orderByAsc(JournalEntry::getTradeDate)
                .orderByAsc(JournalEntry::getId));

        BigDecimal bookBalance = BigDecimal.ZERO;
        BigDecimal unreconciledIncome = BigDecimal.ZERO;
        BigDecimal unreconciledExpenditure = BigDecimal.ZERO;
        List<JournalReconciliationDtos.EntryRow> rows = new java.util.ArrayList<>();
        for (JournalEntry entry : entries) {
            boolean opening = "o".equalsIgnoreCase(entry.getDirection());
            boolean incomeSide = opening || "i".equalsIgnoreCase(entry.getDirection());
            BigDecimal net = incomeSide ? nz(entry.getIncome()) : nz(entry.getExpenditure()).negate();
            bookBalance = bookBalance.add(net);

            boolean reconciled = opening || "y".equals(entry.getReconciled());
            if (!reconciled) {
                if (incomeSide) {
                    unreconciledIncome = unreconciledIncome.add(nz(entry.getIncome()));
                } else {
                    unreconciledExpenditure = unreconciledExpenditure.add(nz(entry.getExpenditure()));
                }
            }
            rows.add(JournalReconciliationDtos.EntryRow.builder()
                    .id(entry.getId())
                    .tradeDate(entry.getTradeDate() != null
                            ? DateUtils.format(entry.getTradeDate(), "yyyy-MM-dd") : "")
                    .summary(StringUtils.defaultString(
                            StringUtils.isNotBlank(entry.getDescription()) ? entry.getDescription() : entry.getRemark()))
                    .direction(entry.getDirection())
                    .income(entry.getIncome())
                    .expenditure(entry.getExpenditure())
                    .balance(entry.getBalance())
                    .reconciled(reconciled)
                    .opening(opening)
                    .build());
        }

        JournalReconciliation statement = reconciliationMapper.selectOne(
                Wrappers.<JournalReconciliation>lambdaQuery()
                        .eq(JournalReconciliation::getBookId, bookId)
                        .eq(JournalReconciliation::getAccId, accId)
                        .eq(JournalReconciliation::getYearPeriod, yearPeriod)
                        .last("limit 1"));
        BigDecimal statementBalance = statement != null ? statement.getStatementBalance() : null;
        BigDecimal adjusted = statementBalance != null
                ? statementBalance.add(unreconciledIncome).subtract(unreconciledExpenditure)
                : null;

        return JournalReconciliationDtos.ReconciliationVo.builder()
                .accId(accId)
                .accName(account.getAccName())
                .yearPeriod(yearPeriod)
                .bookBalance(bookBalance)
                .statementBalance(statementBalance)
                .remark(statement != null ? statement.getRemark() : null)
                .unreconciledIncome(unreconciledIncome)
                .unreconciledExpenditure(unreconciledExpenditure)
                .adjustedStatement(adjusted)
                .difference(adjusted != null ? bookBalance.subtract(adjusted) : null)
                .entries(rows)
                .build();
    }

    /**
     * 登记/更新银行对账单期末余额（按账户+期间唯一）。
     */
    @Transactional
    public JournalReconciliation saveStatement(String bookId, JournalReconciliationDtos.StatementDto dto) {
        bookSealGuard.assertWritable(bookId);
        JournalAccount account = requireAccount(bookId, dto.getAccId());
        parsePeriod(dto.getYearPeriod());
        JournalReconciliation existing = reconciliationMapper.selectOne(
                Wrappers.<JournalReconciliation>lambdaQuery()
                        .eq(JournalReconciliation::getBookId, bookId)
                        .eq(JournalReconciliation::getAccId, account.getId())
                        .eq(JournalReconciliation::getYearPeriod, dto.getYearPeriod())
                        .last("limit 1"));
        if (existing != null) {
            existing.setStatementBalance(dto.getStatementBalance());
            existing.setRemark(dto.getRemark());
            reconciliationMapper.updateById(existing);
            return existing;
        }
        JournalReconciliation row = JournalReconciliation.builder()
                .bookId(bookId)
                .accId(account.getId())
                .yearPeriod(dto.getYearPeriod())
                .statementBalance(dto.getStatementBalance())
                .remark(dto.getRemark())
                .build();
        reconciliationMapper.insert(row);
        return row;
    }

    /**
     * 勾选/取消勾选流水的对账标记。
     */
    @Transactional
    public int mark(String bookId, JournalReconciliationDtos.MarkDto dto) {
        bookSealGuard.assertWritable(bookId);
        if (dto.getEntryIds() == null || dto.getEntryIds().isEmpty()) {
            throw new BusinessException(400, "未选择流水");
        }
        String flag = Boolean.TRUE.equals(dto.getReconciled()) ? "y" : "n";
        int count = 0;
        for (String id : dto.getEntryIds()) {
            JournalEntry entry = entryMapper.selectById(id);
            if (entry == null || !bookId.equals(entry.getBookId())) {
                throw new BusinessException(404, "流水不存在或不属于当前账套");
            }
            JournalEntry update = new JournalEntry();
            update.setId(id);
            update.setReconciled(flag);
            entryMapper.updateById(update);
            count++;
        }
        return count;
    }

    private JournalAccount requireAccount(String bookId, String accId) {
        if (StringUtils.isBlank(accId)) {
            throw new BusinessException(400, "请选择资金账户");
        }
        JournalAccount account = journalAccountService.getById(accId);
        if (account == null || !bookId.equals(account.getBookId())) {
            throw new BusinessException(404, "资金账户不存在或不属于当前账套");
        }
        return account;
    }

    private static YearMonth parsePeriod(String yearPeriod) {
        if (StringUtils.isBlank(yearPeriod)) {
            throw new BusinessException(400, "请选择对账期间");
        }
        try {
            return YearMonth.parse(yearPeriod);
        } catch (Exception e) {
            throw new BusinessException(400, "期间格式应为 yyyy-MM");
        }
    }

    private static Date endOfMonth(YearMonth ym) {
        LocalDate day = ym.atEndOfMonth();
        return Date.from(day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
