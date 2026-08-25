package com.jinbooks.repository.hr;

import com.jinbooks.repository.hr.EmployeeMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.hr.Employee;
import com.jinbooks.dto.hr.EmployeePageDto;
import com.jinbooks.dto.hr.ListNoCalCurrentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    Page<Employee> pageList(Page page, @Param("Dto") EmployeePageDto dto);

    List<Employee> listNoCalCurrent(@Param("Dto") ListNoCalCurrentDto dto);
}
