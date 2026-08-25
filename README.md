<p align="center" >
    <img src="images/logo.png?raw=true"  width="150px"   alt=""/>
</p>
<p align="center">
  <strong>JinBooks-功能齐全的财务记账系统，告别Excel手工记账</strong>
</p>  

# 概述

<b>JinBooks</b>财务记账系统-功能齐全，告别Excel手工记账，产品覆盖财务记账的整个生命周期、实现记账无纸化办公，满足中小企业日常财务记账需求。财务人员录入凭证、日记账、员工管理等；月末自动结转；结账后自动生成资产负债表、利润表、现金流量表、科目余额表等；仪表盘统计让企业管理者对企业财务状况一览无余。

官方网站  <a href="http://www.jinbooks.com" target="_blank"><b>http://www.jinbooks.com</b></a> 

付费服务支持或商务合作：

<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/weixin.jpg?raw=true" width="200px" />


 
# 产品功能


| 序号    | 功能    |  备注    | 
| --------| :-----  |  :-----  | 
| 1.1     | 账套管理        | 无限账套| 
| 1.2     | 科目管理        |  资产、负债、共同、所有者权益、成本、损益类| 
| 1.3     | 凭证管理        |  科目选择、辅助核算、借贷平衡测算、凭证字自动生成、凭证整理| 
| 1.4     | 薪资管理        |  员工管理、薪资自动计算、计提/发放工资凭证| 
| 1.5     | 日记账          |  账户管理、月末汇总| 
| 1.6     | 结账            |   月末结转、结账、报表生成| 
| 1.7     | 报表            |  资产负债表、利润表、现金流量表、科目余额表等| 


# 界面

### 账套管理
<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/jinbooks_book.png?raw=true"/>


### 科目管理
<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/jinbooks_kemu.png?raw=true"/>

### 仪表盘
<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/jinbooks_dashbord.png?raw=true"/>

### 凭证
<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/jinbooks_pingzheng.png?raw=true"/>

<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/jinbooks_pingzheng_gl.png?raw=true"/>

### 报表
#### 科目余额表
<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/jinbooks_rpt_kmey.png?raw=true"/>

#### 资产负债表
<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/jinbooks_rpt_zcfz.png?raw=true"/>

#### 利润表
<img src="https://gitee.com/jinbooks/jinbooks/raw/main/images/jinbooks_rpt_lrb.png?raw=true"/>

# 技术框架

基于Java EE平台，采用Spring、MySQL、Tomcat等开源技术，具有良好扩展性。 

## 后端技术

| 框架                     | 说明            | 版本     |
|------------------------|---------------|--------|
| Spring Boot            | 系统框架             | 4.1.1 |
| Mysql Connector        | 数据库驱动           | 8.4.0 |
| HikariCP               | JDBC 连接池（Spring Boot 默认） | — |
| Jackson                | JSON 工具（Spring Boot 默认） | 3.x |
| MyBatis Plus           | MyBatis 增强工具包   | 3.5.16 | 
| SpringDoc OpenAPI      | API 文档（Swagger UI） | 3.1.0 |
| Hutool                 | Java工具类库         | 5.8.33 | 
| Lombok                 | 消除冗长的 Java 代码 | 1.18.34 | 

## 构建与运行

后端使用 **Maven** 单模块工程，JDK **17+**：

```bash
cd jinbooks
mvnw.cmd -DskipTests package
java -jar target/jinbooks-boot-1.1.0-ga.jar
```

本地 MySQL（WSL Docker）：

```bash
docker-compose up -d
```

默认连接 `127.0.0.1:3307`（避免与本机 3306 冲突），库名 `jinbooks`，用户/密码见 `docker-compose.yml`。

默认端口 `2154`，API 前缀 `/api`。数据库等配置见 `jinbooks/src/main/resources/application.yml`，可通过环境变量 `DATABASE_HOST`、`DATABASE_PORT`、`DATABASE_NAME` 等覆盖。

## 前端技术

| 框架           | 说明        | 版本     |
|--------------|-----------|--------|
| vue          | 框架语言      |3.4.31 |
| vite         | 构建工具      |5.3.2 |
| element-plus | 组件库        |2.9.9 |
| echarts      | 报表库        |5.6.0 |

