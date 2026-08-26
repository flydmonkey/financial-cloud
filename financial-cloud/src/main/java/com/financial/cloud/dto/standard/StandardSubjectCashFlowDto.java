package com.financial.cloud.dto.standard;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

@Data
public class StandardSubjectCashFlowDto {

    @NotEmpty(message = MessageKeys.Validation.BOOK_SUBJECT_CODE_REQUIRED)
    String subjectCode;

    String itemCodeMain;

    String itemCodeSupple;

    String direction;

    String bookId;
}
