package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FixedAssetDisposeResultVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String assetId;
    private String voucherId;
    private String voucherWord;
    private BigDecimal bookValue;
    private BigDecimal disposeIncome;
    private BigDecimal disposeExpense;
    private BigDecimal gainOrLoss;
}
