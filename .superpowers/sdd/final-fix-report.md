
# Final-fix report (whole-branch review)

1. SQL chain: `fixed-asset-check.sql` CREATE TABLE now includes surplus_amount/surplus_voucher_id/surplus_asset_id; `fixed-asset-surplus-booking.sql` made idempotent (information_schema + PREPARE); not registered in build_init_sql.py (columns already in CREATE; comment added). Regenerated init via `python tools/build_init_sql.py`. Regeneration also exposed hand-edit drift: `2026-09-29-voucher-settlement-params.sql` (menu + config) was in init but unregistered; registered in MENU_SEED_SQL so init keeps it (content unchanged, only reordered).
2. Docs: upgrade note + bump_qty known limitation in docs/product/07-fixed-asset.md.
3. UI: check.vue closes surplus dialog when preview is empty (toast only).
4. bookOneSurplus re-selects item by id in-tx and re-checks surplusVoucherId (not a row lock; reduces race window). Tests stub selectById; new concurrent-booked test added.
5. Tests: FixedAssetCheckServiceTest 24/24, FixedAssetServiceSurplusVoucherTest 4/4 pass.
