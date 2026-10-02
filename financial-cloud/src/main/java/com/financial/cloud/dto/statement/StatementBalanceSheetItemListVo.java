package com.financial.cloud.dto.statement;

import com.financial.cloud.domain.statement.StatementBalanceSheetItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceSheetItemListVo implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -8705446953882946583L;

	/**
     * 资产行
     */
    List<StatementBalanceSheetItem> assets;

    /**
     * 负载行
     */
    List<StatementBalanceSheetItem> liability;

    /** 期末试算平衡；缺少总计时为 null，不代表平衡。 */
    private Boolean balanced;
    private BigDecimal assetTotal;
    private BigDecimal liabilityTotal;
    /** 资产总计减负债及权益总计，保留符号。 */
    private BigDecimal balanceDifference;



}
