package com.financial.cloud.enums;

import com.financial.cloud.constants.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum ExcelErrorCode implements ErrorCode {

    TEMPLATE_ROW_NOT_FOUND(502007, MessageKeys.Excel.TEMPLATE_ROW_NOT_FOUND),
    FIELD_NOT_FOUND(502008, MessageKeys.Excel.FIELD_NOT_FOUND);

    private final int code;
    private final String messageKey;

    ExcelErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
