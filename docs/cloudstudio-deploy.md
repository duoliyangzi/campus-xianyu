# CloudStudio 部署步骤（成员 C · 今天就能做）

课程文档《9-1 CloudStudio 项目部署实操》的流程可用；本仓库已准备好脚本。

---

## 你要亲手做的（约 15 分钟）

### 1. 注册并打开工作空间

1. 打开课程同款入口：https://cloudstudio.net/ ，点 **注册登录** / **Cloud Studio 云端 IDE**。  
2. 注册登录后，**新建工作空间**，模板尽量选 **带 Docker** 的 Ubuntu / 全栈类。  
3. 代码导入二选一：  
   - **从 GitHub 导入**本仓库（推荐，先把 `D:\xianyu` 推到 GitHub）；或  
   - 在工作空间里用终端 `git clone <你们仓库地址>`；或  
   - 把本地 `sql/`、`docker-compose.yml`、`start.sh` 打包上传（今天够用）。

### 2. 在 CloudStudio 终端执行

```bash
cd <项目根目录>    # 能看到 sql/、docker-compose.yml、start.sh 的目录
chmod +x start.sh
./start.sh
```

成功标志：终端里打印出分类列表（教材、数码…），且提示 MySQL 已就绪。

### 3. 把连接信息发给 A、B

同一工作空间内后端配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/campus_xianyu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root_change_me
```

**先改掉默认密码**（改 `docker-compose.yml` 里的 `MYSQL_ROOT_PASSWORD`，再重建数据卷），再发给组员。

### 4. 以后有前端/后端时

- A/B 把 `backend/`、`frontend/` 合进仓库后，再跑一次 `./start.sh`  
- 在 CloudStudio「端口」里把 **5173（前端）**、**8080（后端）** 设为可访问 / 公开预览  
- 用预览链接或手机浏览器打开

---

## 今天验收（做到这些就算「已上云」）

- [ ] CloudStudio 工作空间能打开  
- [ ] `./start.sh` 跑通，MySQL 有表  
- [ ] `SELECT * FROM category;` 能看到公共分类  
- [ ] 连接信息已私发给组员  

整站 H5 公网链接可以等前后端代码齐了再发，不必卡在今天。

---

## 常见问题

**没有 Docker？**  
换带 Docker 的模板；或：

```bash
sudo apt update && sudo apt install -y mysql-server
sudo mysql < sql/01_schema.sql
sudo mysql < sql/02_seed_data.sql
```

**机时不够？**  
CloudStudio 免费有时长限制；答辩前若不够，再迁到腾讯云轻量机，SQL 原样导入即可。

**和腾讯云冲突吗？**  
不冲突。现在用 CloudStudio 开发联调；有服务器后再迁，GitHub 里的 `sql/` 始终是准绳。
