package com.financial.cloud.service.statement;

import com.financial.cloud.domain.statement.StatementBalanceSheetItem;
import com.financial.cloud.enums.statement.AssetOrLiabilityEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 资产负债表测试用最小平衡 seed：
 * 1100 资产 8000，1199 资产合计；2200 负债 8000，2299 负债合计。
 */
final class StatementBalanceSheetSeed {

    private StatementBalanceSheetSeed() {
    }

    static List<StatementBalanceSheetItem> balancedItems() {
        List<StatementBalanceSheetItem> items = new ArrayList<>();
        items.add(item(AssetOrLiabilityEnum.asset.name(), "1100", "流动资产", 1, null, bd("8000"), bd("8000")));
        items.add(item(AssetOrLiabilityEnum.asset.name(), "1199", "资产总计", 1, null, BigDecimal.ZERO, BigDecimal.ZERO));
        items.add(item(AssetOrLiabilityEnum.liability.name(), "2200", "流动负债", 1, null, bd("8000"), bd("8000")));
        items.add(item(AssetOrLiabilityEnum.liability.name(), "2299", "负债和所有者权益总计", 1, null, BigDecimal.ZERO, BigDecimal.ZERO));
        return items;
    }

    private static StatementBalanceSheetItem item(String side, String code, String name, int level,
                                                  String parentCode, BigDecimal current, BigDecimal initial) {
        return StatementBalanceSheetItem.builder()
                .bookId("book-seed")
                .balanceSheetId("sheet-seed")
                .assetOrLiability(side)
                .itemCode(code)
                .itemName(name)
                .level(level)
                .parentItemCode(parentCode)
                .currentBalance(current)
                .initialBalance(initial)
                .build();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
