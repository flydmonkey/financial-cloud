package com.jinbooks.controller.hr;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.hr.EmployeeSalaryTemp;
import com.jinbooks.dto.hr.CreateSalaryTableDto;
import com.jinbooks.dto.hr.SalaryDetailChangeDto;
import com.jinbooks.dto.hr.SalaryDetailPageDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.hr.EmployeeSalaryTempService;
import com.jinbooks.validation.EditGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/5 16:51
 */

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
