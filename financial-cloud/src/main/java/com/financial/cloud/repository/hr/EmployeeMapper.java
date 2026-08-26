package com.financial.cloud.repository.hr;

import com.financial.cloud.repository.hr.EmployeeMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.hr.Employee;
import com.financial.cloud.dto.hr.EmployeePageDto;
import com.financial.cloud.dto.hr.ListNoCalCurrentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    Page<Employee> pageList(Page page, @Param("Dto") EmployeePageDto dto);

    List<Employee> listNoCalCurrent(@Param("Dto") ListNoCalCurrentDto dto);
}
