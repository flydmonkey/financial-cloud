package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.financial.cloud.domain.statement.StatementBalanceSheetItem;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementBalanceSheetItemListVo;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.standard.StandardStatementBalanceSheetMapper;
import com.financial.cloud.repository.standard.StandardStatementRulesMapper;
import com.financial.cloud.repository.statement.StatementBalanceSheetItemMapper;
import com.financial.cloud.repository.statement.StatementBalanceSheetMapper;
import com.financial.cloud.repository.statement.StatementRulesMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.service.config.ConfigSysService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class StatementBalanceSheetServiceTest {

    @Mock
    private ConfigSysService configSysService;
    @Mock
    private StatementBalanceSheetMapper balanceSheetMapper;
    @Mock
    private StatementBalanceSheetItemMapper balanceSheetItemMapper;
    @Mock
    private IdentifierGenerator identifierGenerator;
    @Mock
    private StatementRulesMapper statementRulesMapper;
    @Mock
    private StandardStatementBalanceSheetMapper standardStatementBalanceSheetMapper;
    @Mock
    private StandardStatementRulesMapper standardStatementRulesMapper;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private StatementRulesMapper rulesMapper;
    @Mock
    private StatementSubjectBalanceMapper subjectBalanceMapper;

    @InjectMocks
    private StatementBalanceSheetService statementBalanceSheetService;

    @Test
    void insertSubtotals_balancedSeed_keepsAssetsEqualToLiabilities() {
        List<StatementBalanceSheetItem> seed = StatementBalanceSheetSeed.balancedItems();

        StatementBalanceSheetItemListVo result = statementBalanceSheetService.insertSubtotals(seed);

        assertNotNull(result);
        assertNotNull(result.getAssets());
        assertNotNull(result.getLiability());

        BigDecimal assetTotal = grandTotal(result.getAssets());
        BigDecimal liabilityTotal = grandTotal(result.getLiability());

        assertEquals(0, assetTotal.compareTo(liabilityTotal));
        assertEquals(0, new BigDecimal("8000").compareTo(assetTotal));
    }

    @Test
    void normalizeByDirection_creditAccountUsesClosingCreditBalance() {
        StatementSubjectBalance subjectBalance = StatementSubjectBalance.builder()
                .direction(SubjectDirectionEnum.CREDIT.getValue())
                .balance(new BigDecimal("-172608.60"))
                .closingBalanceDebit(BigDecimal.ZERO)
                .closingBalanceCredit(new BigDecimal("172608.60"))
                .build();

        BigDecimal normalized = StatementBalanceSheetService.normalizeByDirection(
                subjectBalance.getDirection(),
                subjectBalance.getClosingBalanceDebit(),
                subjectBalance.getClosingBalanceCredit(),
                subjectBalance.getBalance());

        assertEquals(0, new BigDecimal("172608.60").compareTo(normalized));
    }

    private static BigDecimal grandTotal(List<StatementBalanceSheetItem> items) {
        return items.stream()
                .filter(item -> item.getItemCode() != null && item.getItemCode().endsWith("99"))
                .max(Comparator.comparing(StatementBalanceSheetItem::getItemCode))
                .map(StatementBalanceSheetItem::getCurrentBalance)
                .orElse(BigDecimal.ZERO);
    }
}
