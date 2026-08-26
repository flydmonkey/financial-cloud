package com.financial.cloud.dto.voucher;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VoucherItemChangeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @NotNull(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    private String id;

    /**
     * 凭证ID
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_VOUCHER_TARGET_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String voucherId;

    /**
     * 日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date voucherDate;
    /**
     * 摘要
     */
    private String summary;

    /**
     * 会计科目ID
     */
    @NotEmpty(message = MessageKeys.Validation.BOOK_ACCOUNT_SUBJECT_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String subjectId;

    /**
     * 科目名称
     */
    @NotEmpty(message = MessageKeys.Validation.BOOK_ACCOUNT_SUBJECT_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String subjectName;

    /**
     * 科目编号
     */
    @NotEmpty(message = MessageKeys.Validation.BOOK_SUBJECT_NUMBER_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String subjectCode;

    /**
     * 辅助名称
     */
    private String detailedAccounts;

    /**
     * 二级科目编号
     */
    private String detailedSubjectCode;

    /**
     * 借方金额
     */
    private BigDecimal debitAmount;

    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;

    /**
     * 数量
     */
    private Integer num;

    /**
     * 单价
     */
    private Integer price;

    /**
     * 期初累计借方
     */
    private Integer cumulativeDebit;

    /**
     * 期初累计贷方
     */
    private Integer cumulativeCredit;

    /**
     * 结转损益
     */
    private Integer carryForward;

    /**
     * 科目余额
     */
    private BigDecimal subjectBalance;

    /**
     * 辅助核算配置
     */
    private List<VoucherItemAuxiliaryDto> auxiliary;
}
