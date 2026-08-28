package com.financial.cloud.service.statement;

import com.financial.cloud.domain.statement.StatementIncomeItem;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.standard.StandardStatementIncomeMapper;
import com.financial.cloud.repository.standard.StandardStatementRulesMapper;
import com.financial.cloud.repository.statement.StatementIncomeItemMapper;
import com.financial.cloud.repository.statement.StatementIncomeMapper;
import com.financial.cloud.repository.statement.StatementRulesMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.service.config.ConfigSysService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class StatementIncomeServiceTest {

    @Mock
    private ConfigSysService configSysService;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private VoucherItemMapper voucherItemMapper;
    @Mock
    private StatementRulesMapper statementRulesMapper;
    @Mock
    private StatementIncomeMapper statementIncomeMapper;
    @Mock
    private StatementIncomeItemMapper statementIncomeItemMapper;
    @Mock
    private com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator identifierGenerator;
    @Mock
    private StandardStatementIncomeMapper standardStatementIncomeMapper;
    @Mock
    private StandardStatementRulesMapper standardStatementRulesMapper;

    @InjectMocks
    private StatementIncomeService statementIncomeService;

    @Test
    void validateFormulaChain_strictModeThrowsWhenCorrupted() {
        ReflectionTestUtils.setField(statementIncomeService, "strictFormulaValidation", true);
        List<StatementIncomeItem> items = List.of(
                item("1", "80000"),
                item("105", "10000"),
                item("2", "70000"),
                item("3", "70000"),
                item("4", "60000"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> statementIncomeService.validateFormulaChain(items));
        assertEquals(StatementErrorCode.INCOME_STATEMENT_FORMULA_FAILED.getCode(), ex.getCode());
    }

    @Test
    void validateFormulaChain_nonStrictModeAllowsBalancedChain() {
        ReflectionTestUtils.setField(statementIncomeService, "strictFormulaValidation", false);
        List<StatementIncomeItem> items = List.of(
                item("1", "80000"),
                item("101", "0"),
                item("105", "10000"),
                item("2", "70000"),
                item("3", "70000"),
                item("4", "70000"));

        statementIncomeService.validateFormulaChain(items);
    }

    private static StatementIncomeItem item(String code, String amount) {
        return StatementIncomeItem.builder()
                .itemCode(code)
                .symbol("+")
                .currentBalance(new BigDecimal(amount))
                .cumulativeBalance(new BigDecimal(amount))
                .build();
    }
}
