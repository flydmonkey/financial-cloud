package com.financial.cloud.controller.hr;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.common.ExcelImport;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.hr.EmployeeTaxDeduction;
import com.financial.cloud.dto.hr.EmployeeTaxDeductionDto;
import com.financial.cloud.dto.hr.EmployeeTaxDeductionPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.hr.EmployeeTaxDeductionService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/taxdeduction")
@Slf4j
@RequiredArgsConstructor
public class EmployeeTaxDeductionController {
    private final EmployeeTaxDeductionService employeeTaxDeductionService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<EmployeeTaxDeduction>> fetch(EmployeeTaxDeductionPageDto dto) {
        log.debug("fetch {}", dto);
        dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        return employeeTaxDeductionService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<EmployeeTaxDeduction> getById(@PathVariable(name = "id") String id) {
        return Message.ok(employeeTaxDeductionService.getById(id));
    }

    @PostMapping("/add")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody EmployeeTaxDeductionDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
    	return employeeTaxDeductionService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody EmployeeTaxDeductionDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
    	return employeeTaxDeductionService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return employeeTaxDeductionService.delete(dto);
    }

    @PostMapping(value = "/import")
    public Message<String> importing(
            @ModelAttribute("excelImportFile") ExcelImport excelImportFile,
            @CurrentUser UserInfo currentUser) {
        if (excelImportFile.isExcelNotEmpty()) {
        	employeeTaxDeductionService.importFromExcel(excelImportFile,currentUser);
        }

        return new Message<>(Message.SUCCESS);

    }

}
