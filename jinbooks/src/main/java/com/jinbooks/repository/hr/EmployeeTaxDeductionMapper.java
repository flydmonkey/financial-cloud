package com.jinbooks.repository.hr;

import com.jinbooks.repository.hr.EmployeeTaxDeductionMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.hr.EmployeeTaxDeduction;
import com.jinbooks.dto.hr.EmployeeTaxDeductionPageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface EmployeeTaxDeductionMapper extends BaseMapper<EmployeeTaxDeduction> {
    Page<EmployeeTaxDeduction> pageList(Page page, @Param("dto") EmployeeTaxDeductionPageDto dto);
  
}
