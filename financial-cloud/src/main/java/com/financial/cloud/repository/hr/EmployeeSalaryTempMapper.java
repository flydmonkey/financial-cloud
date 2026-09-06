package com.financial.cloud.repository.hr;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.hr.EmployeeSalaryTemp;
import com.financial.cloud.dto.hr.ListNoCalCurrentDto;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeSalaryTempMapper extends BaseMapper<EmployeeSalaryTemp> {
    Page<EmployeeSalaryTemp> pageList(Page<?> page, @Param("dto") SalaryDetailPageDto dto);

    List<EmployeeSalaryTemp> listCurrentMonth(@Param("dto") SalaryDetailPageDto dto);

    int removeCurrentMonth(@Param("dto") ListNoCalCurrentDto dto);
}
