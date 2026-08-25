package com.jinbooks.controller.hr;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.hr.EmployeeSalary;
import com.jinbooks.domain.hr.EmployeeSalarySummary;
import com.jinbooks.dto.hr.SalaryDetailChangeDto;
import com.jinbooks.dto.hr.SalaryDetailPageDto;
import com.jinbooks.dto.hr.SalarySummaryChangeDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.dto.voucher.GenerateVoucherDto;
import com.jinbooks.service.hr.EmployeeSalaryService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/20 17:02
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/employee/salary")
@Slf4j
public class EmployeeSalaryController {

    private final EmployeeSalaryService employeeSalaryService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<EmployeeSalary>> fetch(SalaryDetailPageDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("fetch {}", dto);
        return employeeSalaryService.pageList(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody SalaryDetailChangeDto dto,
                                  @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("-update  {}", dto);
        return employeeSalaryService.update(dto);
    }

    @PutMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody SalaryDetailChangeDto dto,
                                  @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("-save  {}", dto);
        return employeeSalaryService.save(dto);
    }

    @GetMapping("/get/{id}")
    public Message<EmployeeSalary> getById(@PathVariable(name = "id") String id) {
        return Message.ok(employeeSalaryService.getById(id));
    }

    @GetMapping("/summary")
    public Message<EmployeeSalarySummary> summary(SalarySummaryChangeDto dto,@CurrentUser UserInfo currentUser) {
    	if(dto.getBelongDateRange()!= null && dto.getBelongDateRange().length ==2
    			&& dto.getBelongDateRange()[0].equalsIgnoreCase(dto.getBelongDateRange()[1])) {
    		dto.setBelongDate(YearMonth.parse(dto.getBelongDateRange()[0]));
    	}
    	dto.setBookId(currentUser.getBookId());
        return Message.ok(employeeSalaryService.selectSalarySummary(dto));
    }

    @GetMapping("/export")
    public Message<String> exportTaxItems(SalaryDetailPageDto dto,
            HttpServletResponse response, @CurrentUser UserInfo currentUser) {
            dto.setBookId(currentUser.getBookId());
            return employeeSalaryService.exportTaxItems(dto, response);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return employeeSalaryService.delete(dto);
    }


    @PostMapping("/generate-voucher")
    public Message<String> generateVoucher(@Validated @RequestBody GenerateVoucherDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return employeeSalaryService.generateVoucher(dto);
    }
    
    @PostMapping("/delete-voucher")
    public Message<String> deleteVoucher(@Validated @RequestBody GenerateVoucherDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return employeeSalaryService.deleteVoucher(dto);
    }
}
