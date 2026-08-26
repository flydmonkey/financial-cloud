package com.financial.cloud.dto.hr;

import com.financial.cloud.domain.hr.Employee;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;
import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/25 16:38
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculateSalaryDto {
    List<Employee> employees;

    String bookId;

    YearMonth lastMonth;
}
