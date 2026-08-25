package com.jinbooks.controller.hr;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.hr.EmployeeSalarySummary;
import com.jinbooks.dto.hr.SalaryDetailPageDto;
import com.jinbooks.dto.hr.SalarySummaryChangeDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.hr.EmployeeSalarySummaryService;
import com.jinbooks.validation.AddGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/27 17:46
 */

@RestController
@RequestMapping("/api/employee/salary-summary")
@Slf4j
@RequiredArgsConstructor
public class EmployeeSalarySummaryController {

    private final EmployeeSalarySummaryService employeeSalarySummaryService;

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody SalarySummaryChangeDto dto,
                                @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return employeeSalarySummaryService.save(dto);
    }

    @GetMapping(value = {"/fetch"})
    public Message<Page<EmployeeSalarySummary>> fetch(SalaryDetailPageDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("fetch {}", dto);
        return employeeSalarySummaryService.pageList(dto);
    }

    @GetMapping("/summary")
    public Message<EmployeeSalarySummary> summary(SalarySummaryChangeDto dto,@CurrentUser UserInfo currentUser) {
    	dto.setBookId(currentUser.getBookId());
    	if(dto.getBelongDateRange()!= null && dto.getBelongDateRange().length ==2) {
    		dto.setBelongDate(YearMonth.parse(dto.getBelongDateRange()[0]));
    		dto.setStartDateRange(dto.getBelongDateRange()[0]);
    		dto.setEndDateRange(dto.getBelongDateRange()[1]);
    	}
        return Message.ok(employeeSalarySummaryService.selectSalarySummary(dto));
    }

}
