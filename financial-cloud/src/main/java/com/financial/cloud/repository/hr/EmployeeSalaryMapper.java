package com.financial.cloud.repository.hr;

import com.financial.cloud.repository.hr.EmployeeSalaryMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.hr.EmployeeSalary;
import com.financial.cloud.domain.hr.EmployeeSalarySummary;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.dto.hr.SalarySummaryChangeDto;
import com.financial.cloud.dto.hr.TaxDeductionExportVo;
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
