package com.financial.cloud.exception;

public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 956367551874464320L;

    private final int code;
    private final Object[] messageArgs;
    private final String messageOverride;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, (Object[]) null);
    }

    public BusinessException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getMessageKey());
        this.code = errorCode.getCode();
        this.messageArgs = messageArgs;
        this.messageOverride = null;
    }

    /**
     * For errors whose message is already resolved at throw time (e.g. Passay output).
     */
    public BusinessException(int code, String messageOverride) {
        super(messageOverride);
        this.code = code;
        this.messageArgs = null;
        this.messageOverride = messageOverride;
    }

    public int getCode() {
        return code;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }

    public String getMessageOverride() {
        return messageOverride;
    }

    public String resolveMessage() {
        if (messageOverride != null) {
            return messageOverride;
        }
        return ExceptionMessageResolver.resolve(code, messageArgs);
    }

    @Override
    public String getMessage() {
        return resolveMessage();
    }
}
