package com.financial.cloud.dto.hr;

import com.financial.cloud.domain.hr.Employee;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculateSalaryDto {
    List<Employee> employees;

    String bookId;

    YearMonth lastMonth;
}
