package com.financial.cloud.controller.config;


import com.financial.cloud.service.book.BookOwnershipGuard;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config/salary/formula")
@Slf4j
public class ConfigSalaryFormulaController {
    private final BookOwnershipGuard bookOwnershipGuard;

    private final ConfigSalaryFormulaService configSalaryFormulaService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<ConfigSalaryFormula>> fetch(ConfigSalaryFormulaPageDto dto) {
        bookOwnershipGuard.checkRequest("config_salary_formula", dto);
        log.debug("fetch {}", dto);
        return configSalaryFormulaService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<ConfigSalaryFormulaVo> getById(@PathVariable(name = "id") String id) {
        bookOwnershipGuard.checkReference("config_salary_formula", "id", id);
        return configSalaryFormulaService.getById(id);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody ConfigSalaryFormulaChangeDto dto) {
        bookOwnershipGuard.checkRequest("config_salary_formula", dto);
        com.financial.cloud.constants.auth.ProductRoles.requireAdministrator();
        log.debug("save {}", dto);
        return configSalaryFormulaService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody ConfigSalaryFormulaChangeDto dto) {
        bookOwnershipGuard.checkRequest("config_salary_formula", dto);
        com.financial.cloud.constants.auth.ProductRoles.requireAdministrator();
        log.debug("update {}", dto);
        return configSalaryFormulaService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        bookOwnershipGuard.checkRequest("config_salary_formula", dto);
        com.financial.cloud.constants.auth.ProductRoles.requireAdministrator();
        return configSalaryFormulaService.delete(dto);
    }
}
