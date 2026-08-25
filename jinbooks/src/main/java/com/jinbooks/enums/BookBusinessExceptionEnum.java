package com.jinbooks.enums;

import lombok.Getter;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/23 17:18
 */
@Getter
public enum BookBusinessExceptionEnum {

    ILLEGAL_MOVE_ORG(510001, "非法的移动操作"),

    DUPLICATE_SUBJECTSCODE_EXIST(510002, "当前会计准则已存在相同的科目编码，请重新输入"),

    SUB_SUBJECTS_EXISTS(510003, "请先移除/移动当前会计科目下的子科目"),

    SUB_SUBJECTS_ACTIVE(510004, "请先禁用当前会计科目下的活跃子科目"),

    PARENT_ORGS_FORBIDDEN(510005, "请先启用当前子科目的父级会计科目"),

    DUPLICATE_DEEP_LIMIT(510006, "超出最大科目深度10级"),

    DUPLICATE_SUBJECTS_EXIST(510007, "当前会计准则已存在相同的科目名称，请重新输入"),

    DUPLICATE_SETNAME_EXIST(510008, "当前系统已存在相同的账套名称，请重新输入"),

    ALREADY_STANDARD_USED(510009, "当前会计科目已被会计制度使用无法被禁用"),

    ALREADY_SET_USED(510010, "当前会计科目已被账套使用无法被禁用"),

    DISABLE_BEFORE_DELETE(510011, "请先禁用当前数据再进行删除操作"),

    DELETE_HAS_VOUCHER(510012, "当前科目已被使用，不可删除");

    final String msg;

    final Integer code;

    BookBusinessExceptionEnum(Integer code, String msg) {
        this.msg = msg;
        this.code = code;
    }

}
