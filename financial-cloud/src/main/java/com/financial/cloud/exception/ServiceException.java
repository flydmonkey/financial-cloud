package com.financial.cloud.exception;

/**
 * @description: 业务异常
 * @author: orangeBabu
 * @time: 2024/11/14 15:16
 */
public class ServiceException extends RuntimeException{
    private static final long serialVersionUID = 1L;
    private Integer code;
    private String message;
    private String detailMessage;

    public ServiceException() {
    }

    public ServiceException(String message) {
        this.message = message;
    }

    public ServiceException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public String getDetailMessage() {
        return this.detailMessage;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public Integer getCode() {
        return this.code;
    }

    public ServiceException setMessage(String message) {
        this.message = message;
        return this;
    }

    public ServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }
}
