package com.financial.cloud.enums;

import com.financial.cloud.constants.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum VoucherErrorCode implements ErrorCode {

    ITEM_OR_TIME_INVALID(507001, MessageKeys.Voucher.ITEM_OR_TIME_INVALID);

    private final int code;
    private final String messageKey;

    VoucherErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
