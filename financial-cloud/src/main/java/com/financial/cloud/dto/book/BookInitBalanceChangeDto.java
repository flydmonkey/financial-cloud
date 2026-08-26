package com.financial.cloud.dto.book;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BookInitBalanceChangeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    String id;
    String originId;

    String bookId;

    @NotNull(message = MessageKeys.Validation.BOOK_SUBJECT_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer category;

    @NotEmpty(message = MessageKeys.Validation.BOOK_SUBJECT_ENCODING_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    String code;

    @NotEmpty(message = MessageKeys.Validation.BOOK_SUBJECT_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 21, message = MessageKeys.Validation.BOOK_SUBJECT_NAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    String name;

    @NotNull(message = MessageKeys.Validation.BOOK_BALANCE_DIRECTION_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    String direction;

    /**
     * 上级目录
     */
    String parentId;

    /**
     * 编码路径
     */
    String idPath;

    /**
     * 级别
     */
    Integer level;

    /**
     * 余额
     */
    BigDecimal balance;

    /**
     * 年初余额（借方）
     */
    private BigDecimal openingYearBalanceDebit;

    /**
     * 年初余额（贷方）
     */
    private BigDecimal openingYearBalanceCredit;

    /**
     * 借方总金额（元）
     */
    BigDecimal debitAmount;

    /**
     * 贷方总金额（元）
     */
    BigDecimal creditAmount;

    /**
     * 单位
     */
    String unit;

    /**
     * 辅助核算类型，存在则为辅助核算项
     */
    String assistType;

    /**
     * 辅是否为现金流量
     */
    Integer isCash;
}
