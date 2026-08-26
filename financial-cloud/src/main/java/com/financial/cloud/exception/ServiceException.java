package com.financial.cloud.exception;

@Deprecated
public class ServiceException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ServiceException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }
}
