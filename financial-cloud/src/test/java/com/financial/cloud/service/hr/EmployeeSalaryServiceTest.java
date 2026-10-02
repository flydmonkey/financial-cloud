package com.financial.cloud.service.hr;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.hr.EmployeeSalary;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.hr.SalaryDetailChangeDto;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.enums.error.HrErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.SettlementCarryforwardMapper;
import com.financial.cloud.repository.hr.EmployeeMapper;
import com.financial.cloud.repository.hr.EmployeeSalaryMapper;
import com.financial.cloud.repository.voucher.VoucherMapper;
import com.financial.cloud.repository.voucher.VoucherTemplateItemMapper;
import com.financial.cloud.repository.voucher.VoucherTemplateMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.service.voucher.VoucherTemplateService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeSalaryServiceTest {

    private static final String BOOK_ID = "book-1";
    private static final String SALARY_ID = "salary-1";
    private static final String ACCRUAL_ID = "accrual-1";
    private static final String PAYMENT_ID = "payment-1";

    @Mock
    private EmployeeSalaryMapper employeeSalaryMapper;
    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private VoucherTemplateMapper voucherTemplateMapper;
    @Mock
    private VoucherTemplateItemMapper voucherTemplateItemMapper;
    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private VoucherService voucherService;
    @Mock
    private ConfigSysService configSysService;
    @Mock
    private BookSubjectService bookSubjectService;
    @Mock
    private SettlementCarryforwardMapper settlementCarryforwardMapper;
    @Mock
    private VoucherTemplateService voucherTemplateService;

    @InjectMocks
    private EmployeeSalaryService service;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), EmployeeSalary.class);
    }

    @BeforeEach
    void wireBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", employeeSalaryMapper);
    }

    static Stream<Arguments> linkedVouchers() {
        return Stream.of(
                Arguments.of(ACCRUAL_ID, null),
                Arguments.of(null, PAYMENT_ID),
                Arguments.of(ACCRUAL_ID, PAYMENT_ID));
    }

    @ParameterizedTest
    @MethodSource("linkedVouchers")
    void updateRejectsEitherVoucherLinkWithoutWriting(String accrualId, String paymentId) {
        EmployeeSalary original = salary(SALARY_ID, accrualId, paymentId);
        when(employeeSalaryMapper.selectById(SALARY_ID)).thenReturn(original);

        Message<String> result = service.update(change());

        assertEquals(Message.FAIL, result.getCode());
        assertTrue(result.getMessage().contains("先处理并删除对应凭证"));
        assertEquals(accrualId, original.getAccrualVoucherId());
        assertEquals(paymentId, original.getSalaryVoucherId());
        assertEquals(new BigDecimal("4000"), original.getPayAmount());
        verify(employeeSalaryMapper).selectById(SALARY_ID);
        verifyNoMoreInteractions(employeeSalaryMapper);
        verifyNoInteractions(voucherService);
    }

    @Test
    void updateAllowsUnlinkedDetail() {
        when(employeeSalaryMapper.selectById(SALARY_ID)).thenReturn(salary(SALARY_ID, null, null));
        when(employeeSalaryMapper.updateById(any(EmployeeSalary.class))).thenReturn(1);

        Message<String> result = service.update(change());

        assertEquals(Message.SUCCESS, result.getCode());
        ArgumentCaptor<EmployeeSalary> captor = ArgumentCaptor.forClass(EmployeeSalary.class);
        verify(employeeSalaryMapper).updateById(captor.capture());
        assertEquals(SALARY_ID, captor.getValue().getId());
        assertEquals(BOOK_ID, captor.getValue().getBookId());
        assertEquals(new BigDecimal("4500"), captor.getValue().getPayAmount());
        assertNull(captor.getValue().getAccrualVoucherId());
        assertNull(captor.getValue().getSalaryVoucherId());
        verifyNoInteractions(voucherService);
    }

    @Test
    void updateMissingDetailFailsBeforeWriting() {
        BusinessException error = assertThrows(BusinessException.class, () -> service.update(change()));

        assertEquals(HrErrorCode.RECORD_NOT_FOUND.getCode(), error.getCode());
        verify(employeeSalaryMapper).selectById(SALARY_ID);
        verifyNoMoreInteractions(employeeSalaryMapper);
        verifyNoInteractions(voucherService);
    }

    @ParameterizedTest
    @MethodSource("linkedVouchers")
    void deleteRejectsWholeMixedBatchBeforeWriting(String accrualId, String paymentId) {
        List<String> ids = List.of(SALARY_ID, "salary-2");
        EmployeeSalary linked = salary("salary-2", accrualId, paymentId);
        when(employeeSalaryMapper.selectByIds(ids)).thenReturn(List.of(
                salary(SALARY_ID, null, null), linked));

        Message<String> result = service.delete(deleteRequest(ids));

        assertEquals(Message.FAIL, result.getCode());
        assertEquals(accrualId, linked.getAccrualVoucherId());
        assertEquals(paymentId, linked.getSalaryVoucherId());
        verify(employeeSalaryMapper).selectByIds(ids);
        verifyNoMoreInteractions(employeeSalaryMapper);
        verifyNoInteractions(voucherService);
    }

    @Test
    void deleteAllowsWholeUnlinkedBatch() {
        List<String> ids = List.of(SALARY_ID, "salary-2");
        when(employeeSalaryMapper.selectByIds(ids)).thenReturn(List.of(
                salary(SALARY_ID, null, null), salary("salary-2", null, null)));
        when(employeeSalaryMapper.deleteByIds(ids)).thenReturn(2);

        Message<String> result = service.delete(deleteRequest(ids));

        assertEquals(Message.SUCCESS, result.getCode());
        var order = inOrder(employeeSalaryMapper);
        order.verify(employeeSalaryMapper).selectByIds(ids);
        order.verify(employeeSalaryMapper).deleteByIds(ids);
        verifyNoMoreInteractions(employeeSalaryMapper);
        verifyNoInteractions(voucherService);
    }

    @Test
    void deleteMissingMemberRejectsWholeBatchBeforeWriting() {
        List<String> ids = List.of(SALARY_ID, "missing");
        when(employeeSalaryMapper.selectByIds(ids)).thenReturn(List.of(salary(SALARY_ID, null, null)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.delete(deleteRequest(ids)));

        assertEquals(HrErrorCode.RECORD_NOT_FOUND.getCode(), error.getCode());
        verify(employeeSalaryMapper).selectByIds(ids);
        verifyNoMoreInteractions(employeeSalaryMapper);
        verifyNoInteractions(voucherService);
    }

    @Test
    void deleteNormalizesDuplicateIdsBeforeValidationAndMutation() {
        List<String> unique = List.of(SALARY_ID);
        when(employeeSalaryMapper.selectByIds(unique)).thenReturn(List.of(salary(SALARY_ID, null, null)));
        when(employeeSalaryMapper.deleteByIds(unique)).thenReturn(1);

        Message<String> result = service.delete(deleteRequest(List.of(SALARY_ID, SALARY_ID)));

        assertEquals(Message.SUCCESS, result.getCode());
        verify(employeeSalaryMapper).selectByIds(unique);
        verify(employeeSalaryMapper).deleteByIds(unique);
    }

    @Test
    void deleteEmptyBatchDoesNotWrite() {
        assertEquals(Message.FAIL, service.delete(deleteRequest(List.of())).getCode());
        verifyNoInteractions(employeeSalaryMapper, voucherService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 1, 4})
    void deleteVoucherRejectsUnsupportedTypeBeforeReadingOrWriting(Integer type) {
        Message<String> result = service.deleteVoucher(voucherRequest(type));

        assertEquals(Message.FAIL, result.getCode());
        assertTrue(result.getMessage().contains("不支持"));
        verifyNoInteractions(employeeSalaryMapper, voucherService);
    }

    @Test
    void deleteVoucherMissingSalaryFailsWithoutDeleting() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.deleteVoucher(voucherRequest(2)));

        assertEquals(HrErrorCode.RECORD_NOT_FOUND.getCode(), error.getCode());
        verify(employeeSalaryMapper).selectById(SALARY_ID);
        verifyNoMoreInteractions(employeeSalaryMapper);
        verifyNoInteractions(voucherService);
    }

    static Stream<Arguments> blankSelectedLinks() {
        return Stream.of(
                Arguments.of(2, null), Arguments.of(2, ""), Arguments.of(2, "  "),
                Arguments.of(3, null), Arguments.of(3, ""), Arguments.of(3, "  "));
    }

    @ParameterizedTest
    @MethodSource("blankSelectedLinks")
    void deleteVoucherRejectsEmptySelectedLinkWithoutTouchingOtherLink(int type, String emptyLink) {
        EmployeeSalary original = salary(SALARY_ID,
                type == 2 ? emptyLink : ACCRUAL_ID, type == 3 ? emptyLink : PAYMENT_ID);
        when(employeeSalaryMapper.selectById(SALARY_ID)).thenReturn(original);

        Message<String> result = service.deleteVoucher(voucherRequest(type));

        assertEquals(Message.FAIL, result.getCode());
        assertEquals(type == 2 ? PAYMENT_ID : ACCRUAL_ID,
                type == 2 ? original.getSalaryVoucherId() : original.getAccrualVoucherId());
        verify(employeeSalaryMapper).selectById(SALARY_ID);
        verifyNoMoreInteractions(employeeSalaryMapper);
        verifyNoInteractions(voucherService);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void deleteVoucherReturnsUnderlyingRejectionAndKeepsBothLinks(int type) {
        EmployeeSalary original = salary(SALARY_ID, ACCRUAL_ID, PAYMENT_ID);
        when(employeeSalaryMapper.selectById(SALARY_ID)).thenReturn(original);
        Message<String> rejection = new Message<>(Message.FAIL, "已过账的凭证不能删除", "原始失败详情");
        String selectedId = type == 2 ? ACCRUAL_ID : PAYMENT_ID;
        when(voucherService.delete(List.of(selectedId), BOOK_ID)).thenReturn(rejection);
        RecordingTransactionManager transactions = new RecordingTransactionManager();

        Message<String> result = transactionalService(transactions).deleteVoucher(voucherRequest(type));

        assertSame(rejection, result);
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
        assertEquals(ACCRUAL_ID, original.getAccrualVoucherId());
        assertEquals(PAYMENT_ID, original.getSalaryVoucherId());
        verify(employeeSalaryMapper).selectById(SALARY_ID);
        verifyNoMoreInteractions(employeeSalaryMapper);
        verify(voucherService).delete(List.of(selectedId), BOOK_ID);
    }

    @Test
    void deleteVoucherRollsBackPartialUnderlyingFailureWhilePreservingItsMessage() {
        when(employeeSalaryMapper.selectById(SALARY_ID)).thenReturn(salary(SALARY_ID, ACCRUAL_ID, PAYMENT_ID));
        Message<String> partialFailure = new Message<>(Message.FAIL, "删除失败");
        when(voucherService.delete(List.of(ACCRUAL_ID), BOOK_ID)).thenReturn(partialFailure);
        RecordingTransactionManager transactions = new RecordingTransactionManager();

        Message<String> result = transactionalService(transactions).deleteVoucher(voucherRequest(2));

        assertSame(partialFailure, result);
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
        verify(employeeSalaryMapper).selectById(SALARY_ID);
        verifyNoMoreInteractions(employeeSalaryMapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    @SuppressWarnings({"rawtypes", "unchecked"})
    void deleteDraftVoucherClearsOnlySelectedLinkAfterSuccessfulDeletion(int type) {
        EmployeeSalary original = salary(SALARY_ID, ACCRUAL_ID, PAYMENT_ID);
        when(employeeSalaryMapper.selectById(SALARY_ID)).thenReturn(original);
        String selectedId = type == 2 ? ACCRUAL_ID : PAYMENT_ID;
        when(voucherService.delete(List.of(selectedId), BOOK_ID)).thenReturn(Message.ok("删除成功"));
        when(employeeSalaryMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        RecordingTransactionManager transactions = new RecordingTransactionManager();

        Message<String> result = transactionalService(transactions).deleteVoucher(voucherRequest(type));

        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals(1, transactions.commits);
        assertEquals(0, transactions.rollbacks);
        ArgumentCaptor<Wrapper<EmployeeSalary>> captor = ArgumentCaptor.forClass(Wrapper.class);
        var order = inOrder(employeeSalaryMapper, voucherService);
        order.verify(employeeSalaryMapper).selectById(SALARY_ID);
        order.verify(voucherService).delete(List.of(selectedId), BOOK_ID);
        order.verify(employeeSalaryMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<EmployeeSalary> update = (LambdaUpdateWrapper<EmployeeSalary>) captor.getValue();
        String selectedColumn = type == 2 ? "accrual_voucher_id" : "salary_voucher_id";
        String otherColumn = type == 2 ? "salary_voucher_id" : "accrual_voucher_id";
        assertTrue(update.getSqlSet().startsWith(selectedColumn + "="));
        assertFalse(update.getSqlSet().contains(otherColumn));
        assertTrue(update.getSqlSegment().contains("book_id"));
        assertTrue(update.getSqlSegment().contains(selectedColumn));
        assertTrue(update.getParamNameValuePairs().containsValue(SALARY_ID));
        assertTrue(update.getParamNameValuePairs().containsValue(BOOK_ID));
        assertTrue(update.getParamNameValuePairs().containsValue(selectedId));
        assertTrue(update.getParamNameValuePairs().containsValue(null));
        verifyNoMoreInteractions(employeeSalaryMapper, voucherService);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void deleteVoucherThrowsWhenUnlinkFailsForTransactionRollback() throws Exception {
        when(employeeSalaryMapper.selectById(SALARY_ID)).thenReturn(salary(SALARY_ID, ACCRUAL_ID, PAYMENT_ID));
        when(voucherService.delete(List.of(ACCRUAL_ID), BOOK_ID)).thenReturn(Message.ok("删除成功"));
        when(employeeSalaryMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);
        RecordingTransactionManager transactions = new RecordingTransactionManager();

        BusinessException error = assertThrows(BusinessException.class,
                () -> transactionalService(transactions).deleteVoucher(voucherRequest(2)));

        assertEquals(Message.FAIL, error.getCode());
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
        assertTrue(error.getMessage().contains("解除工资凭证关联失败"));
        assertNotNull(EmployeeSalaryService.class.getMethod("deleteVoucher", GenerateVoucherDto.class)
                .getAnnotation(Transactional.class));
        var order = inOrder(employeeSalaryMapper, voucherService);
        order.verify(employeeSalaryMapper).selectById(SALARY_ID);
        order.verify(voucherService).delete(List.of(ACCRUAL_ID), BOOK_ID);
        order.verify(employeeSalaryMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void deleteVoucherPropagatesDeletionExceptionWithoutUnlinking() {
        when(employeeSalaryMapper.selectById(SALARY_ID)).thenReturn(salary(SALARY_ID, ACCRUAL_ID, PAYMENT_ID));
        BusinessException rejection = new BusinessException(Message.FAIL, "已结账期间不允许删除");
        when(voucherService.delete(List.of(ACCRUAL_ID), BOOK_ID)).thenThrow(rejection);
        RecordingTransactionManager transactions = new RecordingTransactionManager();

        assertSame(rejection, assertThrows(BusinessException.class,
                () -> transactionalService(transactions).deleteVoucher(voucherRequest(2))));
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
        verify(employeeSalaryMapper).selectById(SALARY_ID);
        verifyNoMoreInteractions(employeeSalaryMapper);
    }

    private EmployeeSalaryService transactionalService(RecordingTransactionManager transactions) {
        ProxyFactory proxy = new ProxyFactory(service);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (EmployeeSalaryService) proxy.getProxy();
    }

    /** Exercises Spring's commit/rollback decision without opening a database connection. */
    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int commits;
        private int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }

    private static EmployeeSalary salary(String id, String accrualId, String paymentId) {
        EmployeeSalary salary = new EmployeeSalary();
        salary.setId(id);
        salary.setBookId(BOOK_ID);
        salary.setPayAmount(new BigDecimal("4000"));
        salary.setAccrualVoucherId(accrualId);
        salary.setSalaryVoucherId(paymentId);
        return salary;
    }

    private static SalaryDetailChangeDto change() {
        SalaryDetailChangeDto dto = new SalaryDetailChangeDto();
        dto.setId(SALARY_ID);
        dto.setBookId(BOOK_ID);
        dto.setPayAmount(new BigDecimal("4500"));
        return dto;
    }

    private static ListIdsDto deleteRequest(List<String> ids) {
        ListIdsDto dto = new ListIdsDto();
        dto.setListIds(ids);
        return dto;
    }

    private static GenerateVoucherDto voucherRequest(Integer type) {
        GenerateVoucherDto dto = new GenerateVoucherDto();
        dto.setId(SALARY_ID);
        dto.setBookId("client-book-is-not-used-for-deletion");
        dto.setVoucherType(type);
        return dto;
    }
}
