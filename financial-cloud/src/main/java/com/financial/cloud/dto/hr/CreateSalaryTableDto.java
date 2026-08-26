package com.financial.cloud.dto.hr;

import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

@Data
public class CreateSalaryTableDto {

/*    @NotEmpty(message = MessageKeys.Validation.HR_SELECTED_SALARY_FORMULA_REQUIRED)
    private String formulaId;*/

    private String bookId;
}
