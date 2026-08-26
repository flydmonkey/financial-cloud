package com.financial.cloud.dto.report;

import com.financial.cloud.domain.book.BookInitBalance;
import com.financial.cloud.domain.config.ConfigCashFlowBalance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowSubjectBalanceVo {
    List<ConfigCashFlowBalance> configCashFlowBalances;

    List<BookInitBalance> bookInitBalances;
}
