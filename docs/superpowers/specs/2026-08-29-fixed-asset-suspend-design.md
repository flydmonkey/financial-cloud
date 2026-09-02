# 固定资产暂停计提

## 范围

状态 `SUSPENDED` + `suspended_period`；暂停所属期及之后不计提；恢复后清空并继续按原规则计提。

## 规则

- `shouldAccrue`：若存在 `suspended_period` 且 `yearPeriod >= suspended_period` → false
- 暂停：当前账套期写入 `suspended_period`，状态 SUSPENDED，变动「暂停计提」
- 恢复：IN_USE，清空 suspended_period，变动「恢复计提」
- 已清理不可暂停

## API

- `POST /api/fixed-asset/card/suspend/{id}`
- `POST /api/fixed-asset/card/resume/{id}`
