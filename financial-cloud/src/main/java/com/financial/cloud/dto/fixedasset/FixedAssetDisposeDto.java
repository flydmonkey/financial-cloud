package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class FixedAssetDisposeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String assetId;
    private BigDecimal disposeIncome;
    private BigDecimal disposeExpense;
    /** 收入/费用对方科目（如银行存款） */
    private String counterpartSubjectId;
    private String disposalSubjectId;
    private String gainSubjectId;
    private String lossSubjectId;
    private Date voucherDate;
    private String voucherWord;
    private String summary;
}
