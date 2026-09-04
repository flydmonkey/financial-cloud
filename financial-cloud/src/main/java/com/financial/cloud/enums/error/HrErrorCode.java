package com.financial.cloud.enums.error;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum HrErrorCode implements ErrorCode {

    INSURANCE_FUND_CONFIG_REQUIRED(504001, MessageKeys.Hr.INSURANCE_FUND_CONFIG_REQUIRED),
    EMPLOYEE_NOT_FOUND(504002, MessageKeys.Hr.EMPLOYEE_NOT_FOUND),
    RECORD_NOT_FOUND(504003, MessageKeys.Hr.RECORD_NOT_FOUND),
    NO_DATA(504004, MessageKeys.Hr.NO_DATA),
    CUSTOM_PAY_BASE_REQUIRED(504005, MessageKeys.Hr.CUSTOM_PAY_BASE_REQUIRED),
    PAYMENT_EXPORT_NO_DATA(504006, MessageKeys.Hr.PAYMENT_EXPORT_NO_DATA),
    PAYMENT_EXPORT_MISSING_BANK(504007, MessageKeys.Hr.PAYMENT_EXPORT_MISSING_BANK);

    private final int code;
    private final String messageKey;

    HrErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
