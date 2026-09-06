package com.financial.cloud.enums.error;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum JournalErrorCode implements ErrorCode {

    INSUFFICIENT_BALANCE(508001, MessageKeys.Journal.INSUFFICIENT_BALANCE),
    VOUCHER_ALREADY_LINKED(508002, MessageKeys.Journal.VOUCHER_ALREADY_LINKED),
    OPENING_CANNOT_GENERATE(508003, MessageKeys.Journal.OPENING_CANNOT_GENERATE),
    SUBJECT_REQUIRED(508004, MessageKeys.Journal.SUBJECT_REQUIRED),
    SUBJECT_NOT_FOUND(508005, MessageKeys.Journal.SUBJECT_NOT_FOUND),
    ENTRY_LINKED_LOCKED(508006, MessageKeys.Journal.ENTRY_LINKED_LOCKED),
    SUBJECT_SAME_AS_FUND(508007, MessageKeys.Journal.SUBJECT_SAME_AS_FUND);

    private final int code;
    private final String messageKey;

    JournalErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
