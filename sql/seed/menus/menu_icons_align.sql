-- 对齐菜单图标与文案语义（可重复执行）
-- 仅更新 res_style；同时清空误写入 icon 列的值（前端读 res_style）

-- 顶级 / 账簿簇
UPDATE resources SET res_style = 'menus-pingzhengguanli', icon = NULL WHERE id = '1869692874272862209'; -- 凭证
UPDATE resources SET res_style = 'account-book', icon = NULL WHERE id = '2026082817000000001'; -- 账簿
UPDATE resources SET res_style = 'file-text', icon = NULL WHERE id = '1903024792422047745'; -- 明细账
UPDATE resources SET res_style = 'book', icon = NULL WHERE id = '2026082816300000001'; -- 总账
UPDATE resources SET res_style = 'menus-kemuyuebiao', icon = NULL WHERE id = '1886384516205912065'; -- 科目余额表

-- 报表
UPDATE resources SET res_style = 'bank', icon = NULL WHERE id = '1886366126259052545'; -- 资产负债表（原预算图标）
UPDATE resources SET res_style = 'wallet', icon = NULL WHERE id = '2026082814300000001'; -- 费用明细表

-- 薪资 / 配置
UPDATE resources SET res_style = 'menus-gongzizonglan', icon = NULL WHERE id = '981334321270882304'; -- 薪资
UPDATE resources SET res_style = 'calculator', icon = NULL WHERE id = '1894665979168575489'; -- 当月工资计算
UPDATE resources SET res_style = 'menus-zhangtaoguanli', icon = NULL WHERE id = '981334814802051072'; -- 系统设置（原账套管理顶级）
UPDATE resources SET res_style = 'fund', icon = NULL WHERE id = '1899369820127911938'; -- 初始余额
UPDATE resources SET res_style = 'menus-xianjinliuliangxiangmu', icon = NULL WHERE id = '1913072049310191618'; -- 科目现金流量项配置
UPDATE resources SET res_style = 'fund', icon = NULL WHERE id = '1902625741973843969'; -- 现金流量初始余额
UPDATE resources SET res_style = 'control', icon = NULL WHERE id = '981334679749656576'; -- 系统设置（原配置管理）
UPDATE resources SET res_style = 'menus-moban', icon = NULL WHERE id = '1920446221202178049'; -- 凭证模板

-- 固定资产
UPDATE resources SET res_style = 'build', icon = NULL WHERE id = '2026082818000000001'; -- 固定资产（原 office-building 无对应 svg）
UPDATE resources SET res_style = 'idcard', icon = NULL WHERE id = '2026082818000000011'; -- 卡片
UPDATE resources SET res_style = 'appstore', icon = NULL WHERE id = '2026082818000000021'; -- 资产类别
UPDATE resources SET res_style = 'calculator', icon = NULL WHERE id = '2026082818000000031'; -- 计提折旧
UPDATE resources SET res_style = 'unordered-list', icon = NULL WHERE id = '2026082818000000041'; -- 折旧明细表
UPDATE resources SET res_style = 'bar-chart', icon = NULL WHERE id = '2026082818000000051'; -- 折旧汇总表
UPDATE resources SET res_style = 'swap', icon = NULL WHERE id = '2026082818000000061'; -- 资产变动记录
