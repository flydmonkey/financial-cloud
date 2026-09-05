package com.financial.cloud.enums.error;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum UsersBusinessCode implements ErrorCode {

    USER_VERIFY_MOBILE_ABSENT(500005, MessageKeys.User.VERIFY_MOBILE_ABSENT),

    USER_FORBIDDEN(500008, MessageKeys.User.FORBIDDEN),

    USERNAME_USED(500009, MessageKeys.User.USERNAME_USED),

    MOBILE_USED(500010, MessageKeys.User.MOBILE_USED),

    EMAIL_USED(500011, MessageKeys.User.EMAIL_USED),

    ROLE_REQUIRED(500012, MessageKeys.User.ROLE_REQUIRED),

    BOOK_REQUIRED(500013, MessageKeys.User.BOOK_REQUIRED),

    PERMISSION_DENIED(500014, MessageKeys.User.PERMISSION_DENIED);

    private final int code;
    private final String messageKey;

    UsersBusinessCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
