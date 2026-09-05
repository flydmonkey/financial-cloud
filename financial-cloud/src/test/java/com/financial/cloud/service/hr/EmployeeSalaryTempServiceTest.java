package com.financial.cloud.service.hr;

import com.financial.cloud.constants.auth.ConstsUser;
import com.financial.cloud.domain.config.ConfigInsuranceFund;
import com.financial.cloud.domain.config.ConfigPersonalTax;
import com.financial.cloud.domain.hr.Employee;
import com.financial.cloud.domain.hr.EmployeeSalary;
import com.financial.cloud.dto.hr.CalculateSalaryDto;
import com.financial.cloud.repository.config.ConfigInsuranceFundMapper;
import com.financial.cloud.repository.config.ConfigPersonalTaxMapper;
import com.financial.cloud.repository.hr.EmployeeMapper;
import com.financial.cloud.repository.hr.EmployeeSalaryMapper;
import com.financial.cloud.repository.hr.EmployeeSalaryTempMapper;
import com.financial.cloud.repository.hr.EmployeeTaxDeductionMapper;
import com.financial.cloud.service.config.ConfigSysService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
