package com.financial.cloud.enums.error;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum StandardErrorCode implements ErrorCode {

    USED_BY_BOOK(506001, MessageKeys.Standard.USED_BY_BOOK),
    SUBJECT_NOT_FOUND(506002, MessageKeys.Standard.SUBJECT_NOT_FOUND),
    CASH_FLOW_LEAF_SUBJECT_REQUIRED(506003, MessageKeys.Standard.CASH_FLOW_LEAF_SUBJECT_REQUIRED);

    private final int code;
    private final String messageKey;

    StandardErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
