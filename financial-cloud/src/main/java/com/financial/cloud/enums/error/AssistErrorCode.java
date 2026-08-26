package com.financial.cloud.enums.error;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum AssistErrorCode implements ErrorCode {

    CODE_DUPLICATE(509001, MessageKeys.Assist.CODE_DUPLICATE);

    private final int code;
    private final String messageKey;

    AssistErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
