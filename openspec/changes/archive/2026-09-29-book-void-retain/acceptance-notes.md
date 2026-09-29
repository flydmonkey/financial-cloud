# Acceptance notes — book-void-retain

| Area | Result | Notes |
|------|--------|-------|
| Has vouchers | PASS | `BOOK_HAS_DATA_DELETE` before cascade |
| Empty book | PASS | Existing disable-then-delete path unchanged |
| Sealed | PASS | Still blocked by `SEALED_BOOK_DELETE` |
| Unit test | PASS | `BookServiceTest.delete_rejectsWhenBookHasVouchers` |
| UI tip | PASS | Delete confirm warns to seal when data exists |

「作废留存」产品语义 = 封存（status=2）+ 有凭证禁硬删，不新增状态值。
