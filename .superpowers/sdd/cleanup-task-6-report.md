# Cleanup Task 6 Report

## Summary
Replaced custom Snowflake bit-twiddling with Hutool `IdUtil.getSnowflake(workerId, datacenterId)` behind the existing `SnowFlakeId` façade. Trimmed `DateUtils` by removing 20+ methods with zero production references.

## SnowFlakeId
- **Before:** ~210 lines, custom epoch (`1480166465631L`), manual sequence/clock logic.
- **After:** ~115 lines; delegates to `cn.hutool.core.lang.Snowflake`.
- **Mapping:** `machineId` → Hutool `workerId`, `datacenterId` unchanged (`IdUtil.getSnowflake(machineId, datacenterId)`).
- **Callers unchanged:** `IdGenerator`, `ApplicationAutoConfiguration`, tests still use `SnowFlakeId` / `nextId()`.
- **ID shape:** Still 64-bit `long` → string; fits existing DB string/long columns (typically 18–19 decimal digits).

## DateUtils removed (zero `src/main` references)
`compareDate`, `tryParse`, `getTodayOfWeek`, `compareTime` (×2), `getFormtPattern1ToPattern2`, `formatTimestamp`, `parseTimestamp` (×3), `getDayOfWeek`, `getExchangeFormat`, `plugOneDay`, `getNextDay`, `truncateTime`, `getIntervalDays/Months/Years`, `intervalFormatDisplay`, `getIntervalMilliSeconds`, `toUtcDate`, `addMilliseconds`, unused format constants.

## Kept
All methods/constants still referenced from production code (`format`, `parse`, `lastDay`, `getCurrentDate*`, `formatDateTime`, `addDate`, `addMinutes`, `toUtc*`, etc.).

## Verification
```
JAVA_HOME=jdk-17 mvn -q compile -DskipTests   # OK
```

## Commit
`refactor: use Hutool snowflake and trim dead DateUtils APIs`
