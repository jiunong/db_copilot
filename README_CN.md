# DbCopilot

一个基于 Spring Boot 和 Vue.js 构建的轻量级 Web 版 Oracle 数据库管理工具。

## 功能特性

- **多数据源支持**：配置并管理多个 Oracle 数据库连接。
- **Schema & 表浏览器**：轻松浏览数据库 Schema 和表，支持按名称或注释筛选。
- **数据编辑器**：
  - 支持分页查看表数据。
  - 支持行内编辑和保存记录。
  - **批量操作**：提供强大的批量更新工具（固定值、随机数据、序列、数学运算、数据脱敏/掩码）。
- **结构查看器**：查看表列信息、数据类型及是否可为空。
- **元数据管理**：直接在 UI 上查看和编辑表及列的注释。
- **SQL 编辑器**：执行原生 SQL 查询并查看结果。
- **前端界面**：基于 Vue.js 和 Element UI 构建的响应式界面。

## 技术栈

- **后端**：Java 8, Spring Boot 2.6.13
- **前端**：Vue.js 2, Element UI, Axios
- **数据库**：Oracle (ojdbc8)

## 快速开始

### 环境要求

- JDK 1.8
- Maven 3.x

### 配置

在 `src/main/resources/application.properties` 文件中配置数据库连接。你可以使用 `custom.datasource.list` 前缀定义多个数据源。

```properties
server.port=8080

# 配置示例
custom.datasource.list[0].id=LocalOracle
custom.datasource.list[0].name=本地环境
custom.datasource.list[0].url=jdbc:oracle:thin:@//localhost:1521/ORCL
custom.datasource.list[0].username=scott
custom.datasource.list[0].password=tiger
custom.datasource.list[0].driver-class-name=oracle.jdbc.OracleDriver

custom.datasource.list[1].id=Prod
custom.datasource.list[1].name=生产库
custom.datasource.list[1].url=jdbc:oracle:thin:@//192.168.1.100:1521/ORCL
...
```

### 构建与运行

1. **构建项目：**
   ```bash
   mvn clean package
   ```

2. **运行应用：**
   ```bash
   java -jar target/db_copliot-0.0.1-SNAPSHOT.jar
   ```
   或者直接使用 Maven 运行：
   ```bash
   mvn spring-boot:run
   ```

3. **访问 Web 界面：**
   打开浏览器并访问：
   [http://localhost:8080](http://localhost:8080)

## 开源协议

本项目采用 MIT 许可证。
