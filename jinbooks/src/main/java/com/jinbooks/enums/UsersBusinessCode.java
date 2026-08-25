package com.jinbooks.enums;

import lombok.Getter;

@Getter
public enum UsersBusinessCode {

    USER_FORBIDDEN(500008, "账号被禁用"),

    USERNAME_USED(500009, "该登录名称已被使用"),

    MOBILE_USED(500010, "该手机号码已被使用"),

    EMAIL_USED(500011, "该邮箱地址已被使用"),

    USER_VERIFY_MOBILE_ABSENT(500005, "该手机号尚未绑定任何用户");

    final String msg;
    final Integer code;

    UsersBusinessCode(Integer code, String msg) {
        this.msg = msg;
        this.code = code;
    }

}
