package com.financial.cloud.domain.book;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseSubject;

import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serial;


@EqualsAndHashCode(callSuper = true)
@TableName("book_subject")
@Data
public class BookSubject extends BaseSubject{
    @Serial
    private static final long serialVersionUID = -5652938317535496286L;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    String bookId;

    String originalId;

    /*辅助核算数据: 0-否;1-是*/
    Integer isAuxiliary;

    String belongSubjectId;

    @TableField(exist = false)
    Boolean hasVoucher;
}
