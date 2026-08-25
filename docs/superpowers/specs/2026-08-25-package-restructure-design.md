# 设计：包路径重构（domain / dto / controller / repository / service）

**日期：** 2026-08-25  
**状态：** 已确认 1A + 2A，待规格审阅后进入实现计划  
**模块：** `jinbooks/` 单体

---

## 1. 背景与目标

当前 `com.jinbooks` 包职责混杂：

- `entity.*` 同时承载表实体、DTO、VO
- `web.*.controller` 与 `*Endpoint` 分散
- `persistence.mapper` 扁平堆放 66 个 Mapper
- `persistence.service` 与领域模型跨包耦合

**目标：** 按**功能域**统一五层包路径，职责清晰、便于维护与后续模块化。

**已确认决策：**

| 项 | 选择 |
|----|------|
| VO 处理 | **1A**：并入 `dto.{功能}`，`*Vo` 视为响应 DTO，取消独立 `vo` 包 |
| Service 层 | **2A**：同步迁到 `service.{功能}` |
| 推进方式 | **分波试点**（未选一次全迁） |

**非目标：**

- 不改 API URL、`@RequestMapping` 路径
- 不改数据库表名、MyBatis SQL 语义
- 不拆 Maven 多模块（仍为单体 jar）
- 本波不迁 `authn`/`crypto`/`autoconfigure` 横切包（仅迁业务域）

---

## 2. 目标包结构

```
com.jinbooks
├── common/                          # 横切内核（原 entity 根下共享类）
│   ├── BaseEntity.java
│   ├── BaseSubject.java
│   ├── PageQuery.java
│   ├── Message.java
│   └── ...
├── domain.{功能}/                   # 持久化实体（@TableName）
├── dto.{功能}/                      # 入参/出参/查询对象（含原 vo）
├── controller.{功能}/               # REST 接口（含原 *Endpoint → *Controller）
├── repository.{功能}/               # MyBatis Mapper 接口
├── service.{功能}/                  # 业务接口
│   └── impl/                        # 业务实现（或 service.{功能}.impl 扁平，二选一后全项目统一）
└── （保留）authn, autoconfigure, crypto, util, constants, enums, exception, validate, password, web.filter, web.* 非 Controller 设施
```

### 2.1 命名规则

| 类型 | 包 | 类名约定 | 说明 |
|------|-----|----------|------|
| 表实体 | `domain.{功能}` | `Book`, `Voucher` | `@TableName`，继承 `common.BaseEntity` |
| 请求/查询 DTO | `dto.{功能}` | `*PageDto`, `*ChangeDto`, `*QueryDto` | 分页、变更、查询 |
| 响应 DTO | `dto.{功能}` | `*Vo`, `*Export` | **保留类名后缀 Vo**，仅改包路径 |
| Controller | `controller.{功能}` | `*Controller` | 统一后缀；原 `LoginEndpoint` → `LoginController` |
| Mapper | `repository.{功能}` | `*Mapper` | `@Mapper`，继承 `BaseMapper<T>` |
| Service | `service.{功能}` | `*Service` / `impl.*ServiceImpl` | 原 `persistence.service` |

### 2.2 功能域清单（14 个）

| 功能包名 | 涵盖原 entity 子包 | 涵盖原 web 子包 | 备注 |
|----------|-------------------|-----------------|------|
| `book` | `book`, `base` | `web.book` | 账套、科目、结账、期初、辅助核算 |
| `voucher` | `voucher` | `web.voucher` | 凭证、模板 |
| `journal` | `journal` | `web.journal` | 日记账 |
| `statement` | `statement` | `web.statement` | 三大报表 |
| `standard` | `standard` | `web.standard` | 会计准则 |
| `idm` | `idm` | `web.idm` | 用户、组织、角色成员 |
| `config` | `config` + 根 `Institutions` 等 | `web.config` | 系统/薪资/税务配置 |
| `security` | 根 `SocialsProvider` 等 | `web.security` | 登录策略、邮件/SMS、密码策略 |
| `permissions` | `permissions` | `web.permissions`, `web.access` | 权限、资源、会话列表 |
| `hr` | `hr` | `web.hr` | 员工、薪资 |
| `history` | `history` | `web.historys` | 审计/登录历史 |
| `report` | `report`, `fund` | `web.controller` Dashboard | 看板、资金统计 |
| `auth` | `dto` 中登录相关、`ForgotPassword` | `LoginEndpoint`, `LogoutEndpoint`, `ImageCaptchaEndpoint`, `FileStorageEndpoint`, `MetadataEndpoint`, `ProductVersionEndpoint` | 认证与公共入口 |
| `common` | `entity` 根、`dto` 跨域、`client` | — | 非业务域共享 |

`approval` → 并入 `voucher` 或 `book`（实现时按 Mapper 归属定）。

---

## 3. 迁移映射示例（voucher）

| 现路径 | 目标路径 |
|--------|----------|
| `entity.voucher.Voucher` | `domain.voucher.Voucher` |
| `entity.voucher.dto.VoucherPageDto` | `dto.voucher.VoucherPageDto` |
| `entity.voucher.vo.VoucherVo` | `dto.voucher.VoucherVo` |
| `web.voucher.controller.VoucherController` | `controller.voucher.VoucherController` |
| `persistence.mapper.VoucherMapper` | `repository.voucher.VoucherMapper` |
| `persistence.service.VoucherService` | `service.voucher.VoucherService` |
| `persistence.service.impl.VoucherServiceImpl` | `service.voucher.impl.VoucherServiceImpl` |
| `resources/.../mapper/xml/mysql/VoucherMapper.xml` | `resources/.../repository/voucher/xml/mysql/VoucherMapper.xml` |

XML 内 `namespace` 必须改为新 Mapper 全限定名。

---

## 4. 配置变更

### 4.1 MyBatis

**`MybatisPlusConfiguration.java`：**

```java
@MapperScan(basePackages = "com.jinbooks.repository")
```

（分波期间可临时双扫：`com.jinbooks.persistence.mapper` + `com.jinbooks.repository`，收尾后只保留后者。）

**`application-jinbooks.properties`：**

```properties
# 收尾后示例（按域列举或使用通配）
mybatis-plus.type-aliases-package=com.jinbooks.domain.voucher,com.jinbooks.domain.book,...
# 或简化为（若 MP 支持通配扫描实体包）
mybatis-plus.mapper-locations=classpath*:com/jinbooks/repository/**/xml/${mybatis-plus.dialect}/*.xml
```

删除不存在的 `entity.apps`、`entity.openapi`、`entity.sync` 别名配置。

### 4.2 组件扫描

`@SpringBootApplication` 在 `com.jinbooks` 下，**无需改 scan**；新包均在子包内。

### 4.3 不动部分

- `web.filter`、`GlobalExceptionHandler`、`WebContext` 暂留 `com.jinbooks.web`（非 Controller）
- `authn.*` 会话/JWT 基础设施本波不迁（仅 `auth` 控制器迁入 `controller.auth`）
- `autoconfigure`、`configuration` 保持

---

## 5. 分波实施顺序

| 波次 | 范围 | 验收 |
|------|------|------|
| **0** | 创建 `common`；迁 `Message`/`PageQuery`/`BaseEntity`；更新 MyBatis 双扫配置 | compile |
| **1** | **voucher** 全栈（domain/dto/controller/repository/service + XML） | compile + `/voucher/fetch` 冒烟 |
| **2** | **book**（含 base） | compile + `/book/fetch` |
| **3** | **journal** + **statement** | compile + 报表 API 抽样 |
| **4** | **idm** + **permissions** + **security** | compile + 登录/用户 API |
| **5** | **config** + **hr** + **standard** + **history** + **report** | compile |
| **6** | **auth** 控制器重命名；删空 `entity/`、`persistence.mapper`、`persistence.service`、`web/**/controller` | 全量 package + 阶段 3 冒烟清单 |

每波一个 commit（或每域一个 commit），便于回滚。

---

## 6. 工具与纪律

1. **优先 IDE Refactor → Move**，保证 import 与 XML namespace 一致  
2. 每波结束：`.\mvnw.cmd -DskipTests compile`（收尾 `package`）  
3. grep 残留：`com.jinbooks.entity.`、`persistence.mapper`、`web\.book\.controller` 等应为 0（未迁域除外）  
4. 不改 `@RequestMapping` 值；仅改 Java 包与类引用  
5. `*Endpoint` 重命名为 `*Controller` 时更新 Spring 白名单注释（若有）

---

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| XML namespace 漏改 | 每域迁完 grep `namespace=` 与 Mapper 接口 FQCN |
| type-aliases 漏注册 | 分波期双配置；收尾统一为 `domain.*` 列表或通配 |
| 跨域 Service 循环依赖 | 保持原依赖方向；common 不放业务逻辑 |
| 前端无影响 | URL 不变；仅后端包路径 |
| 一次改动面大 | 严格分波；先 voucher 试点 |

---

## 8. 验收标准（全部完成后）

- [ ] 无 `com.jinbooks.entity.{book,voucher,...}` 业务包（仅 `common` 或已删除 `entity`）
- [ ] 无 `com.jinbooks.persistence.mapper` / `persistence.service`
- [ ] 无 `com.jinbooks.web.{book,voucher,...}.controller`
- [ ] 所有 REST 类在 `controller.{功能}` 下，后缀 `Controller`
- [ ] 所有 `@TableName` 类在 `domain.{功能}` 下
- [ ] 所有 DTO/Vo 在 `dto.{功能}` 下（无独立 `vo` 包）
- [ ] 所有 Mapper 在 `repository.{功能}` 下
- [ ] 所有 Service 在 `service.{功能}` 下
- [ ] `mvnw -DskipTests package` 成功
- [ ] health / login / captcha / 凭证 / 账套 API 冒烟通过

---

## 9. 决策记录

- VO：**1A** — 并入 `dto.{功能}`，保留 `*Vo` 类名  
- Service：**2A** — `service.{功能}` 与四层同步迁移  
- 节奏：**分波试点**，从 voucher 开始  
