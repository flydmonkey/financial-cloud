package com.jinbooks.repository.hr;

import com.jinbooks.repository.hr.EmployeeSalaryMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.hr.EmployeeSalary;
import com.jinbooks.domain.hr.EmployeeSalarySummary;
import com.jinbooks.dto.hr.SalaryDetailPageDto;
import com.jinbooks.dto.hr.SalarySummaryChangeDto;
import com.jinbooks.dto.hr.TaxDeductionExportVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/20 11:56
 */

@Mapper
public interface EmployeeSalaryMapper extends BaseMapper<EmployeeSalary> {
    Page<EmployeeSalary> pageList(Page page, @Param("Dto") SalaryDetailPageDto dto);

    EmployeeSalarySummary selectSalarySummary(@Param("Dto") SalarySummaryChangeDto dto);

    EmployeeSalarySummary selectSalarySummaryLabor(@Param("Dto") SalarySummaryChangeDto dto);
    
    int countEmployeeSalaries(@Param("Dto") SalarySummaryChangeDto dto);

    List<TaxDeductionExportVo> exportGetSalaryDetail(@Param("Dto") SalaryDetailPageDto dto);
}
