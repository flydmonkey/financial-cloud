package com.financial.cloud.controller.hr;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.hr.Employee;
import com.financial.cloud.dto.hr.EmployeeChangeDto;
import com.financial.cloud.dto.hr.EmployeePageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.hr.EmployeeService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
