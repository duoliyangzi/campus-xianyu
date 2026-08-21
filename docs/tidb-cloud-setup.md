# 使用 TiDB Cloud（免费 Starter）作为云端数据库

比 Cloud Studio 里自建 MySQL 更适合「三人共用一个云库」。

## 结论

**可以走 TiDB Cloud Starter（免费额度），推荐作为今天的云端数据库。**

- 兼容 MySQL 协议，Spring Boot 用现有 JDBC 即可  
- 有公网连接串，本地 IDEA / Cloud Studio 都能连  
- 免费额度对课程项目通常够用（注意别绑卡乱开消费上限）

若你说的「tibidata」是别的产品，把官网链接发我再核对；国内课程组最常指的是 **TiDB Cloud**。

## 免费注意点

- 每个组织有免费 Starter 实例额度（存储 + 每月 RU）  
- 超免费额度可能限流或按量计费 → 创建时把 **消费上限设为 0 / 选 Free**  
- 密码、连接串不要提交 GitHub  

## 创建步骤（你来点网页，约 10 分钟）

1. 打开 [TiDB Cloud](https://tidbcloud.com/) 注册/登录  
2. 创建 **Starter** 集群（区域选离你们近的，如新加坡/东京/若有国内区可选）  
3. 设置 root 密码，记下  
4. 在控制台拿到：
   - Host  
   - Port（一般 4000）  
   - User  
   - 是否需要下载 CA 证书（部分环境要 TLS）  
5. 用控制台 SQL Editor，或本地客户端执行仓库里的：

```text
sql/01_schema.sql
sql/02_seed_data.sql
```

6. （可选）用管理账号在 TiDB 里再建 `xianyu_app` 业务账号；若暂时只有一个用户，三人开发阶段先共用一个强密码也可，但 **不要用该密码推公开仓库**  
7. Spring Boot 连接示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://<HOST>:4000/campus_xianyu?useSSL=true&serverTimezone=Asia/Shanghai
    username: <用户名>
    password: <密码>
    driver-class-name: com.mysql.cj.jdbc.Driver
```

端口以控制台为准（常见为 **4000**，不是 3306）。

## 和本仓库 SQL 的兼容性

`01_schema.sql` 按 MySQL 8 写法，TiDB 一般可直接导入。若某条报错：

- `CREATE USER IF NOT EXISTS` 等语法差异 → 在控制台手动建用户  
- 个别索引/引擎提示可忽略或删掉再执行  

导入成功验收：

```sql
SHOW TABLES;
SELECT id, name FROM category;
SELECT id, name FROM campus;
```

## 和 Cloud Studio / 云主机怎么搭配

| 组件 | 建议 |
|------|------|
| 数据库 | **TiDB Cloud 免费 Starter**（今天就定） |
| 写代码 | 本地 Cursor / Cloud Studio 都行 |
| 最终 H5+后端公网 | 仍建议轻量云主机或学生机部署 Jar + Nginx |

也就是说：**库走 TiDB；应用部署另找一台能公网访问的机器**（不一定今天就要）。
