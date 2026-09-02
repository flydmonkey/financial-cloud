ALTER TABLE `fixed_asset`
  ADD COLUMN `suspended_period` varchar(7) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '暂停计提起始期yyyy-MM' AFTER `disposed_period`;
