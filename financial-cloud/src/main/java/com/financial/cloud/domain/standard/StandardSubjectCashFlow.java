package com.financial.cloud.domain.standard;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/4/18 10:05
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "standard_subject_cash_flow")
public class StandardSubjectCashFlow extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1705305465180125164L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    /**
     * 现金流量项编码
     */
    String itemCode;

    /**
     * 科目编码
     */
    String subjectCode;

    /**
     * 余额方向
     */
    String direction;

    /**
     * 准则ID
     */
    String standardId;

    /**
     * 账套ID
     */
    String bookId;

    /**
     * 是否为模板:0-否;1-是
     */
    Integer isTemplate;
}
