package com.financial.cloud.enums;

import com.financial.cloud.constants.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum JournalErrorCode implements ErrorCode {

    INSUFFICIENT_BALANCE(508001, MessageKeys.Journal.INSUFFICIENT_BALANCE);

    private final int code;
    private final String messageKey;

    JournalErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
