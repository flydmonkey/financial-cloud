package com.financial.cloud.service.hr;

import com.financial.cloud.constants.auth.ConstsUser;
import com.financial.cloud.domain.config.ConfigInsuranceFund;
import com.financial.cloud.domain.config.ConfigPersonalTax;
import com.financial.cloud.domain.hr.Employee;
import com.financial.cloud.domain.hr.EmployeeSalary;
import com.financial.cloud.domain.hr.EmployeeSalaryTemp;
import com.financial.cloud.common.Message;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.dto.hr.CalculateSalaryDto;
import com.financial.cloud.repository.config.ConfigInsuranceFundMapper;
import com.financial.cloud.repository.config.ConfigPersonalTaxMapper;
import com.financial.cloud.repository.hr.EmployeeMapper;
import com.financial.cloud.repository.hr.EmployeeSalaryMapper;
import com.financial.cloud.repository.hr.EmployeeSalaryTempMapper;
import com.financial.cloud.repository.hr.EmployeeTaxDeductionMapper;
import com.financial.cloud.service.config.ConfigSysService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeSalaryTempServiceTest {

    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private ConfigInsuranceFundMapper configInsuranceFundMapper;
    @Mock
    private EmployeeSalaryTempMapper employeeSalaryTempMapper;
    @Mock
    private EmployeeSalaryMapper employeeSalaryMapper;
    @Mock
    private EmployeeTaxDeductionMapper employeeTaxDeductionMapper;
    @Mock
    private ConfigPersonalTaxMapper configPersonalTaxMapper;
    @Mock
    private ConfigSysService configSysService;
    @Mock
    private PayrollWriteLock payrollWriteLock;

    @InjectMocks
    private EmployeeSalaryTempService service;

    @Test
    void calculateSalaryAppliesCumulativePitToZeroPayWageEmployeesWithOneHistoryQuery() {
        Employee first = wageEmployee("employee-1");
        Employee second = wageEmployee("employee-2");
        when(configInsuranceFundMapper.selectList(any())).thenReturn(List.of(zeroRateInsuranceFund()));
        when(employeeTaxDeductionMapper.selectList(any())).thenReturn(List.of());
        when(configPersonalTaxMapper.selectList(any())).thenReturn(List.of(wageBracket()));
        when(employeeSalaryMapper.selectList(any())).thenReturn(List.of(
                priorSalary("employee-1"),
                priorSalary("employee-2")));

        var rows = service.calculateSalary(new CalculateSalaryDto(
                List.of(first, second), "book-1", YearMonth.of(2026, 2)));

        assertEquals(2, rows.size());
        assertEquals(0, bd("10000.00").compareTo(rows.get(0).getTaxableWages()));
        assertEquals(0, bd("10000.00").compareTo(rows.get(1).getTaxableWages()));
        assertEquals(0, bd("0.00").compareTo(rows.get(0).getPersonalTax()));
        verify(employeeSalaryMapper).selectList(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"accrual", "payment"})
    void replacementUsesLockedCurrentMonthAndRejectsEitherCommittedLink(String link) {
        SalaryDetailPageDto dto = replacementRequest();
        stubPreview(dto);
        EmployeeSalary current = currentSalary("book-1");
        if ("accrual".equals(link)) {
            current.setAccrualVoucherId("committed-accrual");
        } else {
            current.setSalaryVoucherId("committed-payment");
        }
        when(employeeSalaryMapper.selectActiveByMonthForUpdate("book-1", "2026-09"))
                .thenReturn(List.of(current));

        Message<String> result = service.createFinalDetail(dto);

        assertEquals(Message.FAIL, result.getCode());
        var order = inOrder(payrollWriteLock, configSysService, employeeSalaryTempMapper, employeeSalaryMapper);
        order.verify(payrollWriteLock).lockBook("book-1");
        order.verify(configSysService).getCurrentTerm("book-1");
        order.verify(employeeSalaryTempMapper).listCurrentMonth(dto);
        order.verify(employeeSalaryMapper).selectActiveByMonthForUpdate("book-1", "2026-09");
        verifyNoMoreInteractions(employeeSalaryMapper);
        verify(employeeSalaryMapper, never()).countActiveRowsWithLinkedVouchers(any(), any());
        assertEquals("accrual".equals(link) ? "committed-accrual" : "committed-payment",
                "accrual".equals(link) ? current.getAccrualVoucherId() : current.getSalaryVoucherId());
    }

    @Test
    void replacementAllowsUnlinkedCurrentRowsOnlyAfterBookLockAndCurrentRead() {
        SalaryDetailPageDto dto = replacementRequest();
        stubPreview(dto);
        when(employeeSalaryMapper.selectActiveByMonthForUpdate("book-1", "2026-09"))
                .thenReturn(List.of(currentSalary("book-1")));

        assertEquals(Message.SUCCESS, service.createFinalDetail(dto).getCode());

        var order = inOrder(payrollWriteLock, configSysService, employeeSalaryTempMapper, employeeSalaryMapper);
        order.verify(payrollWriteLock).lockBook("book-1");
        order.verify(configSysService).getCurrentTerm("book-1");
        order.verify(employeeSalaryTempMapper).listCurrentMonth(dto);
        order.verify(employeeSalaryMapper).selectActiveByMonthForUpdate("book-1", "2026-09");
        order.verify(employeeSalaryMapper).delete(any());
        order.verify(employeeSalaryMapper).insert(anyList());
        verifyNoMoreInteractions(employeeSalaryMapper);
        assertEquals(YearMonth.of(2026, 9), dto.getCurrentYearMonth());
    }

    @Test
    void replacementRejectsForeignCurrentRowsBeforeDeletingOrInserting() {
        SalaryDetailPageDto dto = replacementRequest();
        stubPreview(dto);
        when(employeeSalaryMapper.selectActiveByMonthForUpdate("book-1", "2026-09"))
                .thenReturn(List.of(currentSalary("other-book")));

        assertThrows(BusinessException.class, () -> service.createFinalDetail(dto));

        verify(employeeSalaryMapper).selectActiveByMonthForUpdate("book-1", "2026-09");
        verifyNoMoreInteractions(employeeSalaryMapper);
    }

    @Test
    void replacementRejectsForeignPreviewBeforeCheckingOrMutatingConfirmedRows() {
        SalaryDetailPageDto dto = replacementRequest();
        when(configSysService.getCurrentTerm("book-1")).thenReturn("2026-09");
        EmployeeSalaryTemp foreign = new EmployeeSalaryTemp();
        foreign.setBookId("other-book");
        when(employeeSalaryTempMapper.listCurrentMonth(dto)).thenReturn(List.of(foreign));

        assertThrows(BusinessException.class, () -> service.createFinalDetail(dto));

        verify(employeeSalaryMapper, never()).selectActiveByMonthForUpdate(any(), any());
        verifyNoMoreInteractions(employeeSalaryMapper);
    }

    private void stubPreview(SalaryDetailPageDto dto) {
        when(configSysService.getCurrentTerm("book-1")).thenReturn("2026-09");
        EmployeeSalaryTemp preview = new EmployeeSalaryTemp();
        preview.setId("preview-1");
        preview.setBookId("book-1");
        preview.setEmployeeId("employee-1");
        preview.setBelongDate(YearMonth.of(2026, 9));
        when(employeeSalaryTempMapper.listCurrentMonth(dto)).thenReturn(List.of(preview));
    }

    private static EmployeeSalary currentSalary(String bookId) {
        EmployeeSalary salary = new EmployeeSalary();
        salary.setId("salary-1");
        salary.setBookId(bookId);
        salary.setBelongDate(YearMonth.of(2026, 9));
        return salary;
    }

    private static SalaryDetailPageDto replacementRequest() {
        SalaryDetailPageDto dto = new SalaryDetailPageDto();
        dto.setBookId("book-1");
        return dto;
    }

    private static Employee wageEmployee(String id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setIdCardNo(id + "-card");
        employee.setEmployeeType(ConstsUser.EMPLOYEE_TYPE.INTERN);
        employee.setPayBasic(BigDecimal.ZERO);
        employee.setPayPost(BigDecimal.ZERO);
        employee.setPayMerit(BigDecimal.ZERO);
        employee.setLaborFee(BigDecimal.ZERO);
        return employee;
    }

    private static EmployeeSalary priorSalary(String employeeId) {
        EmployeeSalary salary = new EmployeeSalary();
        salary.setEmployeeId(employeeId);
        salary.setPayAmount(bd("20000"));
        salary.setTotalSocialInsurance(BigDecimal.ZERO);
        salary.setProvidentFund(BigDecimal.ZERO);
        salary.setTaxDeduction(BigDecimal.ZERO);
        salary.setPersonalTax(bd("300"));
        return salary;
    }

    private static ConfigInsuranceFund zeroRateInsuranceFund() {
        ConfigInsuranceFund fund = new ConfigInsuranceFund();
        fund.setPayBase(BigDecimal.ZERO);
        fund.setEmploymentInjuryPersonal(BigDecimal.ZERO);
        fund.setEndowmentPersonal(BigDecimal.ZERO);
        fund.setMedicalPersonal(BigDecimal.ZERO);
        fund.setMaternityPersonal(BigDecimal.ZERO);
        fund.setUnemploymentPersonal(BigDecimal.ZERO);
        fund.setProvidentFundSupPersonal(BigDecimal.ZERO);
        fund.setSeriousMedicalPersonal(BigDecimal.ZERO);
        fund.setEmploymentInjuryBusiness(BigDecimal.ZERO);
        fund.setEndowmentBusiness(BigDecimal.ZERO);
        fund.setMedicalBusiness(BigDecimal.ZERO);
        fund.setMaternityBusiness(BigDecimal.ZERO);
        fund.setUnemploymentBusiness(BigDecimal.ZERO);
        fund.setProvidentFundSupBusiness(BigDecimal.ZERO);
        fund.setSeriousMedicalBusiness(BigDecimal.ZERO);
        return fund;
    }

    private static ConfigPersonalTax wageBracket() {
        ConfigPersonalTax bracket = new ConfigPersonalTax();
        bracket.setLevel(1);
        bracket.setMinNum(0);
        bracket.setMaxNum(36000);
        bracket.setTaxRate(3);
        bracket.setCalculationDeduction(0D);
        bracket.setType(0);
        return bracket;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
