/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

package com.jinbooks.dto.hr;

import com.jinbooks.common.PageQuery;
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
