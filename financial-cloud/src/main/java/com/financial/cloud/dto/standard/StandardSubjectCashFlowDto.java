package com.financial.cloud.dto.standard;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/4/21 10:48
 */

@Data
public class StandardSubjectCashFlowDto {

    @NotEmpty(message = "科目代码不能为空")
    String subjectCode;

    String itemCodeMain;

    String itemCodeSupple;

    String direction;

    String bookId;
}
