package com.financial.cloud.enums;

import com.financial.cloud.constants.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum BookBusinessExceptionEnum implements ErrorCode {

    ILLEGAL_MOVE_ORG(510001, MessageKeys.Book.ILLEGAL_MOVE),

    DUPLICATE_SUBJECTSCODE_EXIST(510002, MessageKeys.Book.DUPLICATE_SUBJECT_CODE_EXIST),

    SUB_SUBJECTS_EXISTS(510003, MessageKeys.Book.SUB_SUBJECTS_EXISTS),

    SUB_SUBJECTS_ACTIVE(510004, MessageKeys.Book.SUB_SUBJECTS_ACTIVE),

    PARENT_ORGS_FORBIDDEN(510005, MessageKeys.Book.PARENT_SUBJECT_FORBIDDEN),

    DUPLICATE_DEEP_LIMIT(510006, MessageKeys.Book.DUPLICATE_DEEP_LIMIT),

    DUPLICATE_SUBJECTS_EXIST(510007, MessageKeys.Book.DUPLICATE_SUBJECTS_EXIST),

    DUPLICATE_SETNAME_EXIST(510008, MessageKeys.Book.DUPLICATE_SETNAME_EXIST),

    ALREADY_STANDARD_USED(510009, MessageKeys.Book.ALREADY_STANDARD_USED),

    ALREADY_SET_USED(510010, MessageKeys.Book.ALREADY_SET_USED),

    DISABLE_BEFORE_DELETE(510011, MessageKeys.Book.DISABLE_BEFORE_DELETE),

    DELETE_HAS_VOUCHER(510012, MessageKeys.Book.DELETE_HAS_VOUCHER),

    BOOK_SUB_SUBJECTS_ACTIVE(510013, MessageKeys.Book.BOOK_SUB_SUBJECTS_ACTIVE),

    BOOK_DUPLICATE_SUBJECTS_EXIST(510014, MessageKeys.Book.BOOK_DUPLICATE_SUBJECTS_EXIST),

    BOOK_DUPLICATE_SUBJECT_CODE_EXIST(510015, MessageKeys.Book.BOOK_DUPLICATE_SUBJECT_CODE_EXIST),

    ASSIST_ACC_HAS_CHILDREN(510016, MessageKeys.Book.ASSIST_ACC_HAS_CHILDREN),

    ASSIST_ACC_PARENT_HAS_ASSIST(510017, MessageKeys.Book.ASSIST_ACC_PARENT_HAS_ASSIST);

    private final int code;
    private final String messageKey;

    BookBusinessExceptionEnum(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
