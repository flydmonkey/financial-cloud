package com.financial.cloud.repository.hr;

import com.financial.cloud.repository.hr.EmployeeSalarySummaryMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.hr.EmployeeSalarySummary;
import com.financial.cloud.dto.hr.SalarySummaryChangeDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/27 17:44
 */

@Mapper
public interface EmployeeSalarySummaryMapper extends BaseMapper<EmployeeSalarySummary> {

	 EmployeeSalarySummary selectSalarySummary(@Param("Dto") SalarySummaryChangeDto dto);
}
