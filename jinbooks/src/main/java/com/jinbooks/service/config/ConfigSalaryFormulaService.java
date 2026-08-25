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
 

package com.jinbooks.service.config;

import com.jinbooks.service.config.ConfigSalaryFormulaService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigSalaryFormula;
import com.jinbooks.dto.config.ConfigSalaryFormulaChangeDto;
import com.jinbooks.dto.config.ConfigSalaryFormulaPageDto;
import com.jinbooks.dto.config.ConfigSalaryFormulaVo;
import com.jinbooks.dto.common.ListIdsDto;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/8 17:52
 */
public interface ConfigSalaryFormulaService extends IService<ConfigSalaryFormula> {
    Message<Page<ConfigSalaryFormula>> pageList(ConfigSalaryFormulaPageDto dto);

    Message<String> save(ConfigSalaryFormulaChangeDto dto);

    Message<String> update(ConfigSalaryFormulaChangeDto dto);

    Message<ConfigSalaryFormulaVo> getById(String id);

    Message<String> delete(ListIdsDto dto);
}
