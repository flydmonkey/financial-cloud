package com.financial.cloud.controller.hr;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.hr.EmployeeSalary;
import com.financial.cloud.domain.hr.EmployeeSalarySummary;
import com.financial.cloud.dto.hr.SalaryDetailChangeDto;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.dto.hr.SalarySummaryChangeDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.service.hr.EmployeeSalaryService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/export-payment")
    public Message<String> exportPayment(SalaryDetailPageDto dto,
            HttpServletResponse response, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return employeeSalaryService.exportPaymentFile(dto, response);
    }

    @GetMapping("/count")
    public Message<Long> countByBelongDate(SalaryDetailPageDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return Message.ok(employeeSalaryService.countByBelongDate(dto.getBookId(), dto.getBelongDate()));
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
