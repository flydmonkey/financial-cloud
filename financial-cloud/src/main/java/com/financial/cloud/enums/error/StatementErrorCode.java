package com.financial.cloud.enums.error;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum StatementErrorCode implements ErrorCode {

    PERIOD_TYPE_EMPTY(513001, MessageKeys.Statement.PERIOD_TYPE_EMPTY),
    REPORT_DATE_EMPTY(513002, MessageKeys.Statement.REPORT_DATE_EMPTY),
    BOOK_ID_EMPTY(513003, MessageKeys.Statement.BOOK_ID_EMPTY),
    INVALID_PERIOD_TYPE(513004, MessageKeys.Statement.INVALID_PERIOD_TYPE),
    INVALID_QUARTER(513005, MessageKeys.Statement.INVALID_QUARTER),
    INVALID_HALF_YEAR(513006, MessageKeys.Statement.INVALID_HALF_YEAR),
    DATE_RANGE_SIZE(513007, MessageKeys.Statement.DATE_RANGE_SIZE),
    START_DATE_AFTER_END(513008, MessageKeys.Statement.START_DATE_AFTER_END),
    CASH_FLOW_MODIFY_FORBIDDEN(513009, MessageKeys.Statement.CASH_FLOW_MODIFY_FORBIDDEN),
    CASH_FLOW_INIT_REQUIRED(513010, MessageKeys.Statement.CASH_FLOW_INIT_REQUIRED),
    CASH_FLOW_SQL_REQUIRED(513011, MessageKeys.Statement.CASH_FLOW_SQL_REQUIRED),
    UNKNOWN_CASH_FLOW_CODE(513012, MessageKeys.Statement.UNKNOWN_CASH_FLOW_CODE);

    private final int code;
    private final String messageKey;

    StatementErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
