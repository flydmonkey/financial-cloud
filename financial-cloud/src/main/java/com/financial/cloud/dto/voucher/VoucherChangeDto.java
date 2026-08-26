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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VoucherChangeDto {
    /**
     * 主键
     */
    @NotNull(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    private String id;

    /**
     * 凭证字
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_VOUCHER_WORD_REQUIRED, groups = {EditGroup.class})
    private String word;

    /**
     * 字头：“收”、“付”、“转”等
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_VOUCHER_PREFIX_REQUIRED, groups = {AddGroup.class})
    private String wordHead;

    /**
     * 号码
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_VOUCHER_WORD_NUMBER_REQUIRED, groups = {AddGroup.class})
    private Integer wordNum;

    /**
     * 所属账套
     */
    @NotEmpty(message = MessageKeys.Validation.BOOK_OWNER_BOOK_ID_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String bookId;

    /**
     * 公司ID
     */
    private String companyId;

    /**
     * 公司名称
     */
    @NotEmpty(message = MessageKeys.Validation.ORG_COMPANY_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String companyName;

    /**
     * 附单据数量
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_ATTACHMENT_COUNT_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private Integer receiptNum;

    /**
     * 借方总金额（元）
     */
    private BigDecimal debitAmount;

    /**
     * 贷方总金额（元）
     */
    private BigDecimal creditAmount;

    /**
     * 年份
     */
    private Integer voucherYear;

    /**
     * 月份
     */
    private Integer voucherMonth;

    /**
     * 日期
     */
    @NotNull(message = MessageKeys.Validation.COMMON_DATE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date voucherDate;

    /**
     * 是否结转损益：y|n
     */
    private String carryForward;

    /**
     * 审核人ID
     */
    private String auditMemberId;

    /**
     * 审核人姓名
     */
    private String auditMemberName;

    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date auditDate;

    /**
     * 过账人ID
     */
    private String senderId;

    /**
     * 过账人姓名
     */
    private String senderName;

    /**
     * 过账操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date senderDate;

    /**
     * 主管ID
     */
    private String managerId;

    /**
     * 主管姓名
     */
    private String managerName;

    /**
     * 主管操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date managerDate;

    /**
     * 状态：暂存 - draft,审核中 - reviewing，已完成 - completed，被拒绝 - rejected，已取消 - cancelled
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 凭证明细记录
     */
    @NotNull(message = MessageKeys.Validation.VOUCHER_VOUCHER_ITEMS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private List<VoucherItemChangeDto> items;
}
