package com.jinbooks.dto.statement;

import com.jinbooks.util.excel.ExcelExportCfg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 利润表导出模板数据
 *
 * @author wuyan
 * {@code @date} 2025-02-03
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementIncomeExport implements Serializable {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -324664646653443654L;
	private String companyName;
    private String date;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String itemName;
        private Integer rowNum;

        @ExcelExportCfg
        private BigDecimal currentBalance;
        @ExcelExportCfg
        private BigDecimal yearBalance;

    }
}
