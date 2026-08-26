package com.financial.cloud.enums;

import com.financial.cloud.constants.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum CommonErrorCode implements ErrorCode {

    OPERATION_FAILED(502001, MessageKeys.Common.OPERATION_FAILED),
    SORT_PARAM_INVALID(502002, MessageKeys.Common.SORT_PARAM_INVALID),
    FILE_NOT_FOUND(502003, MessageKeys.Common.FILE_NOT_FOUND),
    PARAM_INVALID_FOR_QUERY(502004, MessageKeys.Common.PARAM_INVALID_FOR_QUERY),
    SQL_INJECTION_RISK(502005, MessageKeys.Common.SQL_INJECTION_RISK),
    EXCEL_SUFFIX_ERROR(502006, MessageKeys.Common.EXCEL_SUFFIX_ERROR);

    private final int code;
    private final String messageKey;

    CommonErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
