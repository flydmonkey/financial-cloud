package com.financial.cloud.dto.standard;

import com.financial.cloud.domain.standard.StandardStatementBalanceSheet;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * 资产负债表-列表数据
 *
 * @author wuyan
 * {@code @date} 2025-05-22
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardStatementBalanceSheetListVo implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1049646083961327739L;

	/**
     * 资产行
     */
    List<StandardStatementBalanceSheet> assets;

    /**
     * 负载行
     */
    List<StandardStatementBalanceSheet> liability;



}
