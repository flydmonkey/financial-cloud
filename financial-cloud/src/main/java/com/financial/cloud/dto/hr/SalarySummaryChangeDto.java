package com.financial.cloud.dto.hr;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.YearMonth;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/28 11:12
 */

@Data
public class SalarySummaryChangeDto {

    @NotNull(message = "编辑对象不能为空", groups = {EditGroup.class})
    private String id;

//    @NotEmpty(message = "名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String label;

    private String description;

    private String bookId;

    @NotNull(message = "所属月份不能为空", groups = {AddGroup.class, EditGroup.class})
    private YearMonth belongDate;
    
    String[] belongDateRange;
    
    String startDateRange;
    
    String endDateRange;
}
