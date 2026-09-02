package com.financial.cloud.enums.error;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum FixedAssetErrorCode implements ErrorCode {

    CATEGORY_CODE_DUPLICATE(514001, MessageKeys.FixedAsset.CATEGORY_CODE_DUPLICATE),
    CATEGORY_IN_USE(514002, MessageKeys.FixedAsset.CATEGORY_IN_USE),
    ASSET_CODE_DUPLICATE(514003, MessageKeys.FixedAsset.ASSET_CODE_DUPLICATE),
    ASSET_HAS_DEPR(514004, MessageKeys.FixedAsset.ASSET_HAS_DEPR),
    ASSET_NOT_FOUND(514005, MessageKeys.FixedAsset.ASSET_NOT_FOUND),
    CALC_FIELDS_LOCKED(514006, MessageKeys.FixedAsset.CALC_FIELDS_LOCKED),
    WORK_REQUIRED(514007, MessageKeys.FixedAsset.WORK_REQUIRED),
    NOTHING_TO_ACCRUE(514008, MessageKeys.FixedAsset.NOTHING_TO_ACCRUE),
    REACCRUE_FORBIDDEN(514009, MessageKeys.FixedAsset.REACCRUE_FORBIDDEN),
    SUBJECT_REQUIRED(514010, MessageKeys.FixedAsset.SUBJECT_REQUIRED),
    CATEGORY_NOT_FOUND(514011, MessageKeys.FixedAsset.CATEGORY_NOT_FOUND),
    CHANGE_ITEMS_EMPTY(514012, MessageKeys.FixedAsset.CHANGE_ITEMS_EMPTY),
    CHANGE_NO_DIFF(514013, MessageKeys.FixedAsset.CHANGE_NO_DIFF),
    ALREADY_DISPOSED(514014, MessageKeys.FixedAsset.ALREADY_DISPOSED),
    DISPOSE_SUBJECT_REQUIRED(514015, MessageKeys.FixedAsset.DISPOSE_SUBJECT_REQUIRED),
    ACCELERATED_LIFE_INVALID(514016, MessageKeys.FixedAsset.ACCELERATED_LIFE_INVALID),
    PURCHASE_SUBJECT_REQUIRED(514017, MessageKeys.FixedAsset.PURCHASE_SUBJECT_REQUIRED),
    ALREADY_SUSPENDED(514018, MessageKeys.FixedAsset.ALREADY_SUSPENDED),
    NOT_SUSPENDED(514019, MessageKeys.FixedAsset.NOT_SUSPENDED),
    CANNOT_SUSPEND_DISPOSED(514020, MessageKeys.FixedAsset.CANNOT_SUSPEND_DISPOSED);

    private final int code;
    private final String messageKey;

    FixedAssetErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}
