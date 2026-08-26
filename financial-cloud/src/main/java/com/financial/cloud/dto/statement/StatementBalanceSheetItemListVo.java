package com.financial.cloud.dto.statement;

import com.financial.cloud.domain.statement.StatementBalanceSheetItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
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



}
