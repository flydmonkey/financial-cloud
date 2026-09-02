-- 固定资产清理凭证字段（由 apply_fixed_asset.py 检测后执行，勿直接依赖 IF NOT EXISTS）
ALTER TABLE `fixed_asset`
  ADD COLUMN `dispose_voucher_id` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '清理凭证ID' AFTER `remark`;
