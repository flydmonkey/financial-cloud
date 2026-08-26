package com.financial.cloud.exception;

/**
 * Business error code with a unique numeric code and i18n message key.
 */
public interface ErrorCode {

    int getCode();

    String getMessageKey();

    default BusinessException exception() {
        return new BusinessException(this);
    }

    default BusinessException exception(Object... messageArgs) {
        return new BusinessException(this, messageArgs);
    }
}
