package com.financial.cloud.controller.config;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.config.ConfigSalaryFormula;
import com.financial.cloud.dto.config.ConfigSalaryFormulaChangeDto;
import com.financial.cloud.dto.config.ConfigSalaryFormulaPageDto;
import com.financial.cloud.dto.config.ConfigSalaryFormulaVo;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.service.config.ConfigSalaryFormulaService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/8 18:00
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config/salary/formula")
@Slf4j
public class ConfigSalaryFormulaController {

    private final ConfigSalaryFormulaService configSalaryFormulaService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<ConfigSalaryFormula>> fetch(ConfigSalaryFormulaPageDto dto) {
        log.debug("fetch {}", dto);
        return configSalaryFormulaService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<ConfigSalaryFormulaVo> getById(@PathVariable(name = "id") String id) {
        return configSalaryFormulaService.getById(id);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody ConfigSalaryFormulaChangeDto dto) {
        log.debug("save {}", dto);
        return configSalaryFormulaService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody ConfigSalaryFormulaChangeDto dto) {
        log.debug("update {}", dto);
        return configSalaryFormulaService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return configSalaryFormulaService.delete(dto);
    }
}
