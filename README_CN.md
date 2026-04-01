# DbCopilot

一个基于 Spring Boot 和 Vue.js 构建的轻量级 Web 版多数据库管理工具。
支持 **Oracle**、**MySQL** 和 **达梦 (Dameng/DM)** 数据库。

## 功能特性

- **多数据源支持**：配置并管理多个数据库连接（Oracle, MySQL, 达梦）。
- **Schema & 表浏览器**：轻松浏览数据库、Schema 和表，支持按名称或注释筛选。
- **数据编辑器**：
  - 支持分页查看表数据，支持排序和筛选。
  - 支持行内编辑和保存记录。
  - **批量操作**：提供强大的批量更新工具（固定值、随机数据、序列、数学运算、数据脱敏/掩码）。
- **结构查看器**：查看表列信息、数据类型及是否可为空。
- **元数据管理**：直接在 UI 上查看和编辑表及列的注释。
- **SQL 编辑器**：执行原生 SQL 查询并查看结果。
- **前端界面**：基于 Vue.js 和 Element UI 构建的响应式界面。

## 技术栈

- **后端**：Java 8, Spring Boot 2.6.13
- **前端**：Vue.js 2, Element UI, Axios
- **数据库**：Oracle, MySQL, 达梦 (Dameng)

## 部署

### 1. 本地构建打包

使用 Maven 打包应用：

```bash
mvn clean package -DskipTests
```

这将在 `target/` 目录下生成 `db_copilot-0.0.1-SNAPSHOT.jar`。

### 2. 使用 Docker 部署到 Linux 服务器

#### 步骤 1：上传项目文件

在服务器上创建一个目录（例如 `/opt/db-copilot`）并上传以下文件：
1. 项目源码（至少包含 `Dockerfile`、`src/main/resources/application.yml`）
2. `target/db_copilot-0.0.1-SNAPSHOT.jar`

#### 步骤 2：构建 Docker 镜像

在上传文件的目录下运行以下命令：

```bash
docker build -t db-copilot:v1 .
```

#### 步骤 3：准备数据目录

创建用于持久化配置（数据库连接）和日志的目录：

```bash
mkdir -p /opt/db-copilot/config
mkdir -p /opt/db-copilot/logs
```

#### 步骤 4：运行容器

运行应用并映射配置目录，以持久化保存您的数据库设置：

```bash
docker run -d \
  --name db-copilot \
  -p 18080:18080 \
  --add-host=host.docker.internal:host-gateway \
  -v /opt/db-copilot/config:/app/config \
  -v /opt/db-copilot/logs:/app/logs \
  -e JAVA_OPTS="-Xms512m -Xmx1024m" \
  db-copilot:v1
```

- 访问应用：`http://<您的服务器IP>:18080`
- 数据库配置将保存在 `/opt/db-copilot/config/db-copilot-config.json`。
- 日志文件将保存在 `/opt/db-copilot/logs`。
- 首次启动时若 `/opt/db-copilot/config/application.yml` 不存在，容器会自动生成默认配置文件。

### 3. 关键说明：容器如何连接宿主机数据库

如果数据库部署在宿主机上，**不要在 JDBC URL 中使用 `localhost` 或 `127.0.0.1`**。

原因：容器内的 `localhost` 指向容器自身，而不是宿主机。

请改为以下方式之一：

1. 推荐：使用 `host.docker.internal`

   - MySQL 示例：

   ```text
   jdbc:mysql://host.docker.internal:3306/mysql?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
   ```

   - Oracle 示例：

   ```text
   jdbc:oracle:thin:@//host.docker.internal:1521/ORCL
   ```

2. 备选：直接使用宿主机实际 IP（如 `192.168.x.x`）

> 说明：
> - 在 Docker Desktop（Windows/macOS）中，`host.docker.internal` 通常可直接使用。
> - 在 Linux Docker 中，建议在 `docker run` 中增加：
>   `--add-host=host.docker.internal:host-gateway`
>   本文示例已包含该参数。

### 4. docker-compose 部署（可选）

如果你使用 `docker-compose.yml`，建议补充 `extra_hosts`，保证 Linux 下也能解析宿主机地址：

```yaml
services:
  db-copilot:
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

## 开源协议

本项目采用 MIT 许可证。
