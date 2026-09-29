package com.financial.cloud.domain.expense;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 报销单票据附件：二进制存 file_storage（category='expense'），本表只管关联。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("expense_claim_attachment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaimAttachment extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String bookId;

    /** 报销单ID */
    private String claimId;

    /** 文件存储ID（file_storage.id） */
    private String fileId;

    private String fileName;

    private Long contentSize;

    private String contentType;

    private Integer sortIndex;

    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
