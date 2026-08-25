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
