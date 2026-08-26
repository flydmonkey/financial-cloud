package com.financial.cloud.dto.hr;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.YearMonth;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/13 17:18
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class SalaryDetailPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -7909627836921847993L;

	YearMonth currentYearMonth;

    String bookId;

    String employeeName;

    String employeeNumber;

    String belongDate;

    String label;

    String[] belongDateRange;
}
