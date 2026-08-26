package com.financial.cloud.enums;

import com.financial.cloud.constants.MessageKeys;
import com.financial.cloud.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum OrgsBusinessExceptionEnum implements ErrorCode {
    SUB_USERS_EXISTS(520001, MessageKeys.Org.SUB_USERS_EXISTS),
    SYNC_USERS_EXISTS(520012, MessageKeys.Org.SYNC_USERS_EXISTS),
    SUB_ORGS_EXISTS(520002, MessageKeys.Org.SUB_ORGS_EXISTS),
    ILLEGAL_MOVE_ORG(520003, MessageKeys.Org.ILLEGAL_MOVE),
    SUB_USERS_ACTIVE(520004, MessageKeys.Org.SUB_USERS_ACTIVE),
    SYNC_USERS_ACTIVE(520013, MessageKeys.Org.SYNC_USERS_ACTIVE),
    SUB_ORGS_ACTIVE(520005, MessageKeys.Org.SUB_ORGS_ACTIVE),
    CURRENT_ORGS_ACTIVE(520006, MessageKeys.Org.CURRENT_ORGS_ACTIVE),
    CURRENT_USERS_ACTIVE(520014, MessageKeys.Org.CURRENT_USERS_ACTIVE),
    PARENT_ORGS_FORBIDDEN(520007, MessageKeys.Org.PARENT_ORGS_FORBIDDEN),
    DUPLICATE_ORGS_EXIST(520008, MessageKeys.Org.DUPLICATE_ORGS_EXIST),
    DUPLICATE_ORGSCODE_EXIST(520009, MessageKeys.Org.DUPLICATE_ORGSCODE_EXIST),
    GROUP_ALREADY_USED(520010, MessageKeys.Org.GROUP_ALREADY_USED),
    GROUPS_ACTIVE(520011, MessageKeys.Org.GROUPS_ACTIVE);

    private final int code;
    private final String messageKey;

    OrgsBusinessExceptionEnum(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
