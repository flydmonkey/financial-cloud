package com.financial.cloud.controller.hr;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.hr.EmployeeSalarySummary;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.dto.hr.SalarySummaryChangeDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.hr.EmployeeSalarySummaryService;
import com.financial.cloud.validation.AddGroup;
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
