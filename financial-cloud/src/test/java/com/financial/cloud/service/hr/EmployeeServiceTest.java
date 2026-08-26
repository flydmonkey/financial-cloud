package com.financial.cloud.service.hr;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.hr.Employee;
import com.financial.cloud.dto.hr.EmployeePageDto;
import com.financial.cloud.repository.hr.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private IdentifierGenerator identifierGenerator;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void pageList_returnsPagedEmployees() {
        EmployeePageDto dto = new EmployeePageDto();
        dto.setPageNumber(1);
        dto.setPageSize(10);

        Page<Employee> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(new Employee()));
        page.setTotal(1);

        when(employeeMapper.pageList(any(), any())).thenReturn(page);

        Message<Page<Employee>> result = employeeService.pageList(dto);

        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }
}
