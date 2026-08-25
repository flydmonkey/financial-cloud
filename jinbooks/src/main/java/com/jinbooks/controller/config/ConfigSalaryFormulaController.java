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
 

package com.jinbooks.controller.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigSalaryFormula;
import com.jinbooks.dto.config.ConfigSalaryFormulaChangeDto;
import com.jinbooks.dto.config.ConfigSalaryFormulaPageDto;
import com.jinbooks.dto.config.ConfigSalaryFormulaVo;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.service.config.ConfigSalaryFormulaService;
import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/8 18:00
 */

@RestController
@RequestMapping("/config/salary/formula")
@Slf4j
public class ConfigSalaryFormulaController {
    static final Logger logger = LoggerFactory.getLogger(ConfigSalaryFormulaController.class);

    @Autowired
    ConfigSalaryFormulaService configSalaryFormulaService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<ConfigSalaryFormula>> fetch(ConfigSalaryFormulaPageDto dto) {
        logger.debug("fetch {}", dto);
        return configSalaryFormulaService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<ConfigSalaryFormulaVo> getById(@PathVariable(name = "id") String id) {
        return configSalaryFormulaService.getById(id);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody ConfigSalaryFormulaChangeDto dto) {
        logger.debug("save {}", dto);
        return configSalaryFormulaService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody ConfigSalaryFormulaChangeDto dto) {
        logger.debug("update {}", dto);
        return configSalaryFormulaService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return configSalaryFormulaService.delete(dto);
    }
}
