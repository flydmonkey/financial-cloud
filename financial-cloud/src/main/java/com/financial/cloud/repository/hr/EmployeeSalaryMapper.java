package com.financial.cloud.repository.hr;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.hr.EmployeeSalary;
import com.financial.cloud.domain.hr.EmployeeSalarySummary;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.dto.hr.SalarySummaryChangeDto;
import com.financial.cloud.dto.hr.TaxDeductionExportVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmployeeSalaryMapper extends BaseMapper<EmployeeSalary> {

    Page<EmployeeSalary> pageList(Page<?> page, @Param("dto") SalaryDetailPageDto dto);

    EmployeeSalarySummary selectSalarySummary(@Param("dto") SalarySummaryChangeDto dto);

    EmployeeSalarySummary selectSalarySummaryLabor(@Param("dto") SalarySummaryChangeDto dto);

    int countEmployeeSalaries(@Param("dto") SalarySummaryChangeDto dto);

    List<TaxDeductionExportVo> exportGetSalaryDetail(@Param("dto") SalaryDetailPageDto dto);

    @Select("""
            SELECT COUNT(1) FROM employee_salary
            WHERE book_id = #{bookId}
              AND belong_date = #{belongDate}
              AND deleted = 'n'
              AND (
                (accrual_voucher_id IS NOT NULL AND accrual_voucher_id <> '')
                OR (salary_voucher_id IS NOT NULL AND salary_voucher_id <> '')
              )
            """)
    int countActiveRowsWithLinkedVouchers(@Param("bookId") String bookId,
            @Param("belongDate") String belongDate);

    @Select("""
            SELECT accrual_voucher_id FROM employee_salary
            WHERE book_id = #{bookId}
              AND employee_id = #{employeeId}
              AND belong_date = #{belongDate}
              AND accrual_voucher_id IS NOT NULL AND accrual_voucher_id <> ''
            ORDER BY created_date DESC
            LIMIT 1
            """)
    String findAnyAccrualVoucherId(@Param("bookId") String bookId,
            @Param("employeeId") String employeeId,
            @Param("belongDate") String belongDate);

    @Select("""
            SELECT salary_voucher_id FROM employee_salary
            WHERE book_id = #{bookId}
              AND employee_id = #{employeeId}
              AND belong_date = #{belongDate}
              AND salary_voucher_id IS NOT NULL AND salary_voucher_id <> ''
            ORDER BY created_date DESC
            LIMIT 1
            """)
    String findAnySalaryVoucherId(@Param("bookId") String bookId,
            @Param("employeeId") String employeeId,
            @Param("belongDate") String belongDate);
}
