# Acceptance notes — dashboard-asset-count

| Area | Result | Notes |
|------|--------|-------|
| API | PASS | `GET /api/statistics/fixed-asset-count` |
| Counts | PASS | Excludes DISPOSED; splits IN_USE / SUSPENDED |
| Net value | PASS | original − accumDepr − impairment |
| Unit tests | PASS | `FixedAssetDashboardServiceTest` |
| UI | PASS | Homepage card + quick entry「固定资产」 |

Manual: open homepage with assets → card shows totals; click → `/fixed-asset/card`.
