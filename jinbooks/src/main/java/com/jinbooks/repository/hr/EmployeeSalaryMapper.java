/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

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
