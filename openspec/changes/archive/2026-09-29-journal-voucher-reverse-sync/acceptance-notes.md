# Acceptance notes — journal-voucher-reverse-sync

| Area | Result | Notes |
|------|--------|-------|
| Delete unlinks | PASS | `clearLinksByVoucherIds` after voucher delete |
| Void unlinks | PASS | same clear on void |
| Update sync | PASS | fund debit→income / credit→expenditure + recalc |
| Structure reject | PASS | `VOUCHER_SYNC_STRUCTURE` |
| Unit tests | PASS | `JournalEntryServiceTest` + `VoucherServiceTest` |

Manual: generate voucher from journal → edit voucher amount → journal income updates; delete voucher → journal unlocked without voucherId.
