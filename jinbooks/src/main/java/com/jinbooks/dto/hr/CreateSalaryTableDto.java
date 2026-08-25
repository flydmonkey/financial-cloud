package com.jinbooks.dto.hr;

import lombok.Data;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/14 11:29
 */

@Data
public class CreateSalaryTableDto {

/*    @NotEmpty(message = "所选薪资计算公式不能为空")
    private String formulaId;*/

    private String bookId;
}
