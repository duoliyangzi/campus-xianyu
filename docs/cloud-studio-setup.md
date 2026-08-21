# Cloud Studio 免费版：今天把开发库建起来（成员 C）

## 适不适合

- **适合**：今天统一云端 MySQL、导入表结构与公共数据、三人连同一库开发。  
- **不太适合**：当作最终「发给评委的长期公网地址」。免费有时长限制，工作空间可能休眠，H5 公网访问也不如轻量云主机省心。

## 操作步骤

1. 打开 [Cloud Studio](https://cloudstudio.net/)，用 GitHub 导入本仓库（或新建工作空间后上传本项目）。
2. 确认模板支持 Docker（官方带 Docker 的环境最省事）。
3. 在终端执行：

```bash
cd /path/to/xianyu
# 先改 docker-compose.yml 里的 MYSQL_ROOT_PASSWORD
docker compose up -d
docker compose ps
```

4. 等 healthy 后进入库检查：

```bash
docker exec -it campus-xianyu-mysql mysql -uroot -p -e "USE campus_xianyu; SHOW TABLES; SELECT id,name FROM category;"
```

5. 修改 `sql/00_create_users.sql` 中的两个密码并执行，把 `xianyu_app` 账号密码私发组员（不要公开贴群）。
6. 每人本地 / Cloud Studio 里的 Spring Boot `application.yml` 示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://<CloudStudio可达的主机>:3306/campus_xianyu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: xianyu_app
    password: <运行账号密码>
    driver-class-name: com.mysql.cj.jdbc.Driver
```

注意：若 MySQL 跑在 **同一个** Cloud Studio 工作空间，主机名用 `127.0.0.1` 即可；若三人各自本地 IDEA 连这台库，需要 Cloud Studio 提供端口转发 / 公网访问能力，且务必改强密码、限制来源。**更稳妥的三人共用方式**是：买/领一台学生云主机只跑 MySQL（或 MySQL+后端），Cloud Studio 只当编辑器。

## 若工作空间不能用 Docker

在同环境用 apt 安装 MySQL 8，再手动：

```bash
sudo mysql -uroot -p < sql/01_schema.sql
sudo mysql -uroot -p < sql/02_seed_data.sql
```

## 管理员账号

- seed 里预置了 `admin` 用户，但 `password_hash` 是占位符。  
- A 同学登录模块就绪后：用后端 BCrypt 生成 `ChangeMe@123`（或你们约定的密码）的哈希，执行：

```sql
UPDATE user SET password_hash = '<真实哈希>' WHERE username = 'admin';
```

或删除 seed 中的 admin 行，改用「初始化管理员」接口创建。

## 今天验收清单

- [ ] `SHOW TABLES` 能看到全部业务表  
- [ ] `category` 有教材/数码/…/周边二级  
- [ ] `campus`、`report_reason` 有数据  
- [ ] 已创建 `xianyu_app`，Spring Boot 能连上并 `SELECT`  
- [ ] SQL 已推 GitHub，组员不再私自改表
