package com.financial.cloud.common;

import java.util.Date;

import org.springframework.http.ResponseEntity;

import lombok.Data;

/**
 * 返回信息内容<br>
 * code 返回码 <br>
 * message 消息提示 <br>
 * data 返回数据内容 <br>
 *
 * @param <T>
 */
@Data
public class Message<T> {

    public static final int SUCCESS = 0;    //成功
    public static final int ERROR = 1;    //错误
    public static final int FAIL = 2;    //失败
    public static final int INFO = 101;    //信息
    public static final int PROMPT = 102;    //提示
    public static final int WARNING = 103;    //警告
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    int code;

    String message;

    Date timestamp;

    T data;

    public Message() {
        this.code = SUCCESS;
        this.timestamp = new Date();
    }

    public Message(int code) {
        this.code = code;
        this.timestamp = new Date();
    }

    public Message(T data) {
        this.data = data;
        this.timestamp = new Date();
    }

    public Message(int code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = new Date();
    }

    public Message(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = new Date();
    }

    public Message(int code, T data) {
        this.code = code;
        this.data = data;
        this.timestamp = new Date();
    }

    public static <T> Message<T> ok(T data) {
        return new Message<>(SUCCESS, data);
    }

    public static <T> Message<T> failed(String message) {
        return new Message<>(FAIL, message);
    }

    public void setMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public void setData(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public ResponseEntity<?> buildResponse() {
        return ResponseEntity.ok(this);
    }

    public ResponseEntity<Message<T>> buildUnauthorizedResponse() {
        return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(this);
    }

}
