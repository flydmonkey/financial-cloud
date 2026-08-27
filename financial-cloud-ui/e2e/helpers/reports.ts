import {expect} from '@playwright/test'

export function sheetGrandTotal(items: Array<{itemCode?: string; currentBalance?: number | string}>) {
    const totals = items.filter((item) => (item.itemCode || '').endsWith('99'))
    if (totals.length === 0) {
        return null
    }
    const grand = totals.reduce((max, item) =>
        ((item.itemCode || '') > (max.itemCode || '') ? item : max),
    )
    return Number(grand.currentBalance ?? 0)
}

export function assertBalanceSheetTrial(
    assets: Array<{itemCode?: string; currentBalance?: number | string; itemName?: string}>,
    liability: Array<{itemCode?: string; currentBalance?: number | string; itemName?: string}>,
) {
    expect(assets.length + liability.length).toBeGreaterThan(0)
    expect(assets.some((item) => (item.itemName || '').includes('总计'))).toBeTruthy()
    expect(liability.some((item) => (item.itemName || '').includes('总计'))).toBeTruthy()

    const assetTotal = sheetGrandTotal(assets)
    const liabilityTotal = sheetGrandTotal(liability)
    expect(assetTotal, '资产负债表缺少资产总计行').not.toBeNull()
    expect(liabilityTotal, '资产负债表缺少负债及权益总计行').not.toBeNull()
    expect(Math.abs(assetTotal! - liabilityTotal!)).toBeLessThanOrEqual(0.01)
}
