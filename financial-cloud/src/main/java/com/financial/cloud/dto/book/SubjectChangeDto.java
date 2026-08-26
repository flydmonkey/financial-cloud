package com.financial.cloud.dto.book;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

@Data
public class SubjectChangeDto {

    @NotEmpty(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    String id;

    @NotNull(message = MessageKeys.Validation.BOOK_SUBJECT_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer category;

    @NotEmpty(message = MessageKeys.Validation.BOOK_SUBJECT_ENCODING_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 25, message = MessageKeys.Validation.ORG_CODE_LENGTH_RANGE, groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "[\\d\\-.]+", message = MessageKeys.Validation.BOOK_SUBJECT_CODE_PATTERN, groups = {AddGroup.class, EditGroup.class})
    String code;

    @NotEmpty(message = MessageKeys.Validation.BOOK_SUBJECT_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 21, message = MessageKeys.Validation.BOOK_SUBJECT_NAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    String name;

    String pinyinCode;

    @NotNull(message = MessageKeys.Validation.BOOK_BALANCE_DIRECTION_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    String direction;

    @NotNull(message = MessageKeys.Validation.BOOK_CASH_SUBJECT_FLAG_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer isCash;

    /**
     * 使用范围
     */
    String scope;
    /**
     * 分类
     */
    String classify;

    @NotNull(message = MessageKeys.Validation.COMMON_STATUS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer status;

    String parentId;

    /**
     * 辅助核算
     */
    String auxiliary;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    String standardId;

    String bookId;
}
