package com.jinbooks.controller.hr;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.hr.Employee;
import com.jinbooks.dto.hr.EmployeeChangeDto;
import com.jinbooks.dto.hr.EmployeePageDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.hr.EmployeeService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 员工信息管理接口
 */

@RestController
@RequestMapping("/api/salary/employee")
@Slf4j
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<Employee>> fetch(EmployeePageDto dto,
                                         @CurrentUser UserInfo currentUser) {
        log.debug("fetch {}", dto);
        dto.setBookId(currentUser.getBookId());
        return employeeService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<Employee> getById(@PathVariable(name = "id") String id) {
        return Message.ok(employeeService.getById(id));
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody EmployeeChangeDto dto,
                                @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return employeeService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody EmployeeChangeDto dto) {
        return employeeService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return employeeService.delete(dto);
    }

}
