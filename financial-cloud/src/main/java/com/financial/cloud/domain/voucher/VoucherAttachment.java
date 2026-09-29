package com.financial.cloud.domain.voucher;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 凭证附件（影像）：二进制存于 file_storage，本表保存凭证关联与展示元信息。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("voucher_attachment")
public class VoucherAttachment extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String bookId;

    private String voucherId;

    /** file_storage.id */
    private String fileId;

    private String fileName;

    private Long contentSize;

    private String contentType;

    private Integer sortIndex;

    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
