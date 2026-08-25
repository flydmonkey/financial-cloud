package com.jinbooks.dto.report;

import com.jinbooks.domain.book.BookInitBalance;
import com.jinbooks.domain.config.ConfigCashFlowBalance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/6/3 16:37
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowSubjectBalanceVo {
    List<ConfigCashFlowBalance> configCashFlowBalances;

    List<BookInitBalance> bookInitBalances;
}
