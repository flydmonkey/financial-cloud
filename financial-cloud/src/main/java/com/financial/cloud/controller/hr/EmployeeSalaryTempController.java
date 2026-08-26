package com.financial.cloud.controller.hr;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.hr.EmployeeSalaryTemp;
import com.financial.cloud.dto.hr.CreateSalaryTableDto;
import com.financial.cloud.dto.hr.SalaryDetailChangeDto;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.hr.EmployeeSalaryTempService;
import com.financial.cloud.validation.EditGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/salary/detail")
@Slf4j
public class EmployeeSalaryTempController {

    private final EmployeeSalaryTempService jbxSalaryDetailService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<EmployeeSalaryTemp>> fetch(SalaryDetailPageDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("fetch {}", dto);
        return jbxSalaryDetailService.pageList(dto);
    }

    @PostMapping("/createTable")
    public Message<String> createTable(@Validated @RequestBody CreateSalaryTableDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return jbxSalaryDetailService.createTable(dto);
    }

    @GetMapping("/get/{id}")
    public Message<EmployeeSalaryTemp> getById(@PathVariable(name = "id") String id) {
        return Message.ok(jbxSalaryDetailService.getById(id));
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody SalaryDetailChangeDto dto,
                                  @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("-update  {}", dto);
        return jbxSalaryDetailService.update(dto);
    }

    @PostMapping("/submit-detail")
    public Message<String> createFinalDetail(@CurrentUser UserInfo currentUser) {
        SalaryDetailPageDto dto = new SalaryDetailPageDto();
        dto.setBookId(currentUser.getBookId());
        log.debug("-createFinalDetail  {}", dto);
        return jbxSalaryDetailService.createFinalDetail(dto);
    }

    @GetMapping("/re-calculate")
    public Message<EmployeeSalaryTemp> reCalculate(EmployeeSalaryTemp employeeSalaryTemp) {
        return jbxSalaryDetailService.reCalculate(employeeSalaryTemp);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return jbxSalaryDetailService.delete(dto);
    }
}
