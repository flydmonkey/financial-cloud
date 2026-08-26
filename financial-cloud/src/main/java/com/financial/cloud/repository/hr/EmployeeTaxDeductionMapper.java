package com.financial.cloud.repository.hr;

import com.financial.cloud.repository.hr.EmployeeTaxDeductionMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.hr.EmployeeTaxDeduction;
import com.financial.cloud.dto.hr.EmployeeTaxDeductionPageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface EmployeeTaxDeductionMapper extends BaseMapper<EmployeeTaxDeduction> {
    Page<EmployeeTaxDeduction> pageList(Page page, @Param("dto") EmployeeTaxDeductionPageDto dto);
  
}
