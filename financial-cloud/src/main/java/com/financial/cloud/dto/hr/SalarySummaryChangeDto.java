package com.financial.cloud.dto.hr;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.time.YearMonth;

@Data
public class SalarySummaryChangeDto {

    @NotNull(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    private String id;

//    @NotEmpty(message = MessageKeys.Validation.COMMON_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String label;

    private String description;

    private String bookId;

    @NotNull(message = MessageKeys.Validation.COMMON_OWNER_MONTH_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private YearMonth belongDate;
    
    String[] belongDateRange;
    
    String startDateRange;
    
    String endDateRange;
}
