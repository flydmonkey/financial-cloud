package com.jinbooks.repository.hr;

import com.jinbooks.repository.hr.EmployeeSalaryTempMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.hr.EmployeeSalaryTemp;
import com.jinbooks.dto.hr.SalaryDetailPageDto;
import com.jinbooks.dto.hr.ListNoCalCurrentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/5 16:45
 */

@Mapper
public interface EmployeeSalaryTempMapper extends BaseMapper<EmployeeSalaryTemp> {
    Page<EmployeeSalaryTemp> pageList(Page page, @Param("Dto") SalaryDetailPageDto dto);

    List<EmployeeSalaryTemp> listCurrentMonth(@Param("Dto") SalaryDetailPageDto dto);

    int removeCurrentMonth(@Param("Dto") ListNoCalCurrentDto dto);
}
