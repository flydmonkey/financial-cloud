ALTER TABLE `fixed_asset_check_item`
  ADD COLUMN `surplus_amount` decimal(18,2) DEFAULT NULL COMMENT '盘盈入账金额' AFTER `remark`,
  ADD COLUMN `surplus_voucher_id` varchar(45) DEFAULT NULL COMMENT '盘盈凭证ID' AFTER `surplus_amount`,
  ADD COLUMN `surplus_asset_id` varchar(45) DEFAULT NULL COMMENT '盘盈拆出新卡ID' AFTER `surplus_voucher_id`;
