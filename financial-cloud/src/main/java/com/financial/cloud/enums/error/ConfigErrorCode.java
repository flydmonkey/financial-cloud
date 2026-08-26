package com.financial.cloud.enums.error;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum ConfigErrorCode implements ErrorCode {

    PERSONAL_TAX_RANGE_INVALID(505001, MessageKeys.Config.PERSONAL_TAX_RANGE_INVALID),
    PERSONAL_TAX_LEVEL_DUPLICATE(505002, MessageKeys.Config.PERSONAL_TAX_LEVEL_DUPLICATE),
    SALARY_FORMULA_NAME_DUPLICATE(505003, MessageKeys.Config.SALARY_FORMULA_NAME_DUPLICATE),
    PASSWORD_POLICY_NOT_CONFIGURED(505004, MessageKeys.Config.PASSWORD_POLICY_NOT_CONFIGURED),
    BOOK_NOT_INIT_CURRENT_PERIOD(505005, MessageKeys.Config.BOOK_NOT_INIT_CURRENT_PERIOD),
    BOOK_NOT_INIT_INITIAL_PERIOD(505006, MessageKeys.Config.BOOK_NOT_INIT_INITIAL_PERIOD),
    BUILTIN_PARAM_CANNOT_DELETE(505007, MessageKeys.Config.BUILTIN_PARAM_CANNOT_DELETE),
    BOOK_MISSING_PARAM(505008, MessageKeys.Config.BOOK_MISSING_PARAM);

    private final int code;
    private final String messageKey;

    ConfigErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
