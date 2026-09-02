ALTER TABLE `fixed_asset`
  ADD COLUMN `purchase_voucher_id` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '购入凭证ID' AFTER `dispose_voucher_id`;
