-- 凭证红字冲销：记录冲销凭证的来源凭证
ALTER TABLE `voucher`
  ADD COLUMN `source_voucher_id` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '红字冲销来源凭证ID' AFTER `remark`;

ALTER TABLE `voucher`
  ADD KEY `idx_voucher_source` (`source_voucher_id`);
