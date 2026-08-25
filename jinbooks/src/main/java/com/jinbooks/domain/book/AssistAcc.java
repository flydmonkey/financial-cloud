package com.jinbooks.domain.book;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 辅助核算对象 jbx_assist_acc
 *
 * @author Wuyan
 * {@code @date} 2025-03-10
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("jbx_assist_acc")
public class AssistAcc extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 所属账套
     */
    private String bookId;

    /**
     * 辅助类别
     */
    private String assistType;

    /**
     * 编码
     */
    private String assistCode;

    /**
     * 名称
     */
    private String assistName;

    /**
     * 规格
     */
    private String spec;

    /**
     * 部门
     */
    private String dept;

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

    /**
     * 删除标记
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
