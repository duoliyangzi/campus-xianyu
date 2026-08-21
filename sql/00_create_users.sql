-- ============================================================
-- 数据库账号隔离（在 MySQL 管理账号下执行，导入业务库之前或之后均可）
-- 请修改下方密码后再执行；不要把真实密码提交到公开仓库
-- ============================================================

-- 管理账号：建表 / 改结构 / 导公共数据（仅负责人保管）
CREATE USER IF NOT EXISTS 'xianyu_ddl'@'%' IDENTIFIED BY 'REPLACE_DDL_PASSWORD';
GRANT ALL PRIVILEGES ON campus_xianyu.* TO 'xianyu_ddl'@'%';

-- 项目运行账号：仅业务 CRUD（Spring Boot 使用）
CREATE USER IF NOT EXISTS 'xianyu_app'@'%' IDENTIFIED BY 'REPLACE_APP_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON campus_xianyu.* TO 'xianyu_app'@'%';

FLUSH PRIVILEGES;

-- 说明：
-- 1. Spring Boot application.yml 只配置 xianyu_app
-- 2. 改表结构时用 xianyu_ddl 或 root，执行完同步更新 GitHub 中的 SQL
-- 3. 若 MySQL 版本较旧不支持 IF NOT EXISTS，可改为普通 CREATE USER
