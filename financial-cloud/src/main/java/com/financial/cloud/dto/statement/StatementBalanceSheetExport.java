package com.financial.cloud.dto.statement;

import com.financial.cloud.util.excel.ExcelExportCfg;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceSheetExport implements Serializable {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 8096547651691024294L;
	private String companyName;
    private String date;
    private List<AssetLiability> assetLiabilityList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetLiability {
        private String assetItemName;
        private Integer assetRowNum;

        @ExcelExportCfg
        private BigDecimal assetCurrentBalance;
        @ExcelExportCfg
        private BigDecimal assetInitialBalance;

        private String liabilityItemName;
        private Integer liabilityRowNum;
        @ExcelExportCfg
        private BigDecimal liabilityCurrentBalance;
        @ExcelExportCfg
        private BigDecimal liabilityInitialBalance;
    }
}
