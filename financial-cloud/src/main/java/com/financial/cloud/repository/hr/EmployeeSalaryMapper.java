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
import org.apache.ibatis.annotations.Options;

import java.util.List;

@Mapper
public interface EmployeeSalaryMapper extends BaseMapper<EmployeeSalary> {

    @Select("SELECT id FROM voucher WHERE id = #{voucherId} AND deleted = 'n' FOR UPDATE")
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    String findLiveVoucherIdForUpdate(@Param("voucherId") String voucherId);

    @Select("""
            SELECT * FROM employee_salary
            WHERE id = #{id} AND book_id = #{bookId} AND deleted = 'n'
            FOR UPDATE
            """)
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    EmployeeSalary selectActiveByIdForUpdate(@Param("bookId") String bookId, @Param("id") String id);

    @Select("""
            <script>
            SELECT * FROM employee_salary
            WHERE book_id = #{bookId} AND deleted = 'n' AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            FOR UPDATE
            </script>
            """)
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    List<EmployeeSalary> selectActiveByIdsForUpdate(@Param("bookId") String bookId,
            @Param("ids") List<String> ids);

    @Select("""
            SELECT * FROM employee_salary FORCE INDEX (idx_salary_payroll_scope)
            WHERE book_id = #{bookId} AND belong_date = #{belongDate} AND deleted = 'n'
            ORDER BY id
            FOR UPDATE
            """)
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    List<EmployeeSalary> selectActiveByMonthForUpdate(@Param("bookId") String bookId,
            @Param("belongDate") String belongDate);

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
            SELECT es.accrual_voucher_id FROM employee_salary es FORCE INDEX (idx_salary_payroll_scope)
            STRAIGHT_JOIN voucher v ON es.accrual_voucher_id = v.id
            WHERE es.book_id = #{bookId}
              AND es.employee_id = #{employeeId}
              AND es.belong_date = #{belongDate}
              AND es.accrual_voucher_id IS NOT NULL AND es.accrual_voucher_id <> ''
              AND v.deleted = 'n'
            LIMIT 1
            FOR UPDATE
            """)
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    String findAnyLiveAccrualVoucherId(@Param("bookId") String bookId,
            @Param("employeeId") String employeeId,
            @Param("belongDate") String belongDate);

    @Select("""
            SELECT es.salary_voucher_id FROM employee_salary es FORCE INDEX (idx_salary_payroll_scope)
            STRAIGHT_JOIN voucher v ON es.salary_voucher_id = v.id
            WHERE es.book_id = #{bookId}
              AND es.employee_id = #{employeeId}
              AND es.belong_date = #{belongDate}
              AND es.salary_voucher_id IS NOT NULL AND es.salary_voucher_id <> ''
              AND v.deleted = 'n'
            LIMIT 1
            FOR UPDATE
            """)
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    String findAnyLiveSalaryVoucherId(@Param("bookId") String bookId,
            @Param("employeeId") String employeeId,
            @Param("belongDate") String belongDate);
}
