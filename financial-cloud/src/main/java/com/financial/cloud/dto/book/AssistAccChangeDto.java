package com.financial.cloud.dto.book;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AssistAccChangeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 所属账套
     */
    @NotBlank(message = MessageKeys.Validation.BOOK_OWNER_BOOK_ID_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String bookId;

    /**
     * 辅助核算类型
     */
    @NotBlank(message = MessageKeys.Validation.ASSIST_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String assistType;

    /**
     * 辅助核算编码
     */
    @NotBlank(message = MessageKeys.Validation.ASSIST_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String assistCode;

    /**
     * 辅助核算名称
     */
    @NotBlank(message = MessageKeys.Validation.ASSIST_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String assistName;

    /**
     * 部门
     */
    private String dept;

    /**
     * 规格
     */
    private String spec;

    /**
     * 单位
     */
    private String unit;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否禁用：y/n
     */
    private String status;
}
