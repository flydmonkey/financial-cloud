package com.financial.cloud.service.hr;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.hr.SalaryDetailChangeDto;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.hr.EmployeeSalaryMapper;
import org.apache.ibatis.mapping.MappedStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollWriteLockTest {
    @Mock
    private BookMapper bookMapper;
    @InjectMocks
    private PayrollWriteLock lock;

    @BeforeEach
    void markUnitTransactionActive() {
        // Unit metadata only: production lock SQL still requires a real service
        // transaction, covered separately by the Spring/MySQL integration test.
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void clearUnitTransactionMetadata() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void missingActualTransactionFailsBeforeAcquiringAnAutocommitLock() {
        TransactionSynchronizationManager.setActualTransactionActive(false);

        assertThrows(BusinessException.class, () -> lock.lockBook("book-a"));

        verifyNoInteractions(bookMapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingBookScopeFailsBeforeAnySql(String bookId) {
        assertThrows(BusinessException.class, () -> lock.lockBook(bookId));
        verifyNoInteractions(bookMapper);
    }

    @Test
    void deletedOrMissingBookCannotCoordinatePayrollWrites() {
        assertThrows(BusinessException.class, () -> lock.lockBook("missing-book"));
        verify(bookMapper).lockActiveBookId("missing-book");
        verifyNoMoreInteractions(bookMapper);
    }

    @Test
    void foreignReturnedBookCannotEstablishRequestedScope() {
        when(bookMapper.lockActiveBookId("book-a")).thenReturn("book-b");
        assertThrows(BusinessException.class, () -> lock.lockBook("book-a"));
        verify(bookMapper).lockActiveBookId("book-a");
        verifyNoMoreInteractions(bookMapper);
    }

    @Test
    void activeRequestedBookEstablishesCoordinationThroughTheLockingQuery() {
        when(bookMapper.lockActiveBookId("book-a")).thenReturn("book-a");
        lock.lockBook("book-a");
        verify(bookMapper).lockActiveBookId("book-a");
        verifyNoMoreInteractions(bookMapper);
    }

    @Test
    void currentReadStatementsFlushPriorQueriesAndBypassCachedSnapshots() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(BookMapper.class);
        configuration.addMapper(EmployeeSalaryMapper.class);
        Map<String, Object> parameters = Map.of(
                "bookId", "book-a", "id", "salary-a", "ids", List.of("salary-a", "salary-b"),
                "employeeId", "employee-a", "belongDate", "2026-09", "voucherId", "voucher-a");
        assertCurrentRead(configuration, BookMapper.class, "lockActiveBookId", parameters);
        for (String method : List.of("selectActiveByIdForUpdate", "selectActiveByIdsForUpdate",
                "selectActiveByMonthForUpdate", "findLiveVoucherIdForUpdate",
                "findAnyLiveAccrualVoucherId", "findAnyLiveSalaryVoucherId")) {
            assertCurrentRead(configuration, EmployeeSalaryMapper.class, method, parameters);
        }
    }

    @Test
    void sixCoordinatedEntryPointsUseReadCommittedForOwnedTransactionsAndRequiredJoining() throws Exception {
        assertOwnedTransaction(EmployeeSalaryService.class, "update", SalaryDetailChangeDto.class);
        assertOwnedTransaction(EmployeeSalaryService.class, "save", SalaryDetailChangeDto.class);
        assertOwnedTransaction(EmployeeSalaryService.class, "delete", ListIdsDto.class);
        assertOwnedTransaction(EmployeeSalaryService.class, "generateVoucher", GenerateVoucherDto.class);
        assertOwnedTransaction(EmployeeSalaryService.class, "deleteVoucher", GenerateVoucherDto.class);
        assertOwnedTransaction(EmployeeSalaryTempService.class, "createFinalDetail", SalaryDetailPageDto.class);
    }

    private static void assertOwnedTransaction(Class<?> owner, String method, Class<?> argument) throws Exception {
        Transactional transaction = owner.getMethod(method, argument).getAnnotation(Transactional.class);
        assertNotNull(transaction, method + " must execute within the payroll coordination transaction");
        assertEquals(Isolation.READ_COMMITTED, transaction.isolation(),
                method + " must avoid RR boundary locks when opening its own transaction");
        assertEquals(Propagation.REQUIRED, transaction.propagation(),
                method + " must join the existing caller transaction rather than commit separately");
    }

    private static void assertCurrentRead(MybatisConfiguration configuration, Class<?> mapper,
                                          String method, Map<String, Object> parameters) {
        MappedStatement statement = configuration.getMappedStatement(mapper.getName() + "." + method);
        assertTrue(statement.isFlushCacheRequired(), method + " must invalidate prior query cache");
        assertFalse(statement.isUseCache(), method + " must not use a cached earlier salary state");
        String sql = statement.getBoundSql(parameters).getSql().replaceAll("\\s+", " ").trim();
        assertTrue(sql.endsWith("FOR UPDATE"), method + " must perform a current locking read");
        if (List.of("selectActiveByMonthForUpdate", "findAnyLiveAccrualVoucherId",
                "findAnyLiveSalaryVoucherId").contains(method)) {
            assertTrue(sql.contains("FORCE INDEX (idx_salary_payroll_scope)"),
                    method + " must use the production payroll scope index");
        }
        if (List.of("findAnyLiveAccrualVoucherId", "findAnyLiveSalaryVoucherId").contains(method)) {
            assertTrue(sql.contains("STRAIGHT_JOIN voucher"),
                    method + " must read the scoped salary side before probing the voucher primary key");
        }
    }
}
