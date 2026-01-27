# DbCopilot

A lightweight, web-based multi-database management tool built with Spring Boot and Vue.js.
Supports **Oracle**, **MySQL**, and **Dameng (DM)** databases.

## Features

- **Multi-Database Support**: Configure and manage connections to multiple databases (Oracle, MySQL, Dameng).
- **Schema & Table Browser**: Easily browse databases, schemas, and tables.
- **Data Editor**:
  - View table data with pagination, sorting, and filtering.
  - Inline editing and saving of records.
  - **Batch Operations**: Powerful tools to update multiple rows at once (Fixed value, Random data, Sequence, Math operations, Data Masking).
- **Structure Viewer**: Inspect table columns, data types, and nullability.
- **SQL Editor**: Execute raw SQL queries and view results.
- **Frontend**: Responsive UI built with Vue.js and Element UI.

## Technology Stack

- **Backend**: Java 8, Spring Boot 2.6.13
- **Frontend**: Vue.js 2, Element UI, Axios
- **Databases**: Oracle, MySQL, Dameng

## Getting Started

### Prerequisites

- JDK 1.8
- Maven 3.x

### Configuration

Configure your database connections in `src/main/resources/application.yml`.

```yaml
server:
  port: 18080

custom:
  datasource:
    list:
      - id: LocalOracle
        name: Local Oracle
        url: jdbc:oracle:thin:@//localhost:1521/ORCL
        username: scott
        password: tiger
        driver-class-name: oracle.jdbc.OracleDriver
      
      - id: LocalMySQL
        name: Local MySQL
        url: jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: password
        driver-class-name: com.mysql.cj.jdbc.Driver
      
      - id: DM8
        name: Dameng DB
        url: jdbc:dm://localhost:5236
        username: SYSDBA
        password: SYSDBA
        driver-class-name: dm.jdbc.driver.DmDriver
```

### Building and Running

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Run the application:**
   ```bash
   java -jar target/db_copilot-0.0.1-SNAPSHOT.jar
   ```
   Or run directly with Maven:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the Web UI:**
   Open your browser and navigate to:
   [http://localhost:18080](http://localhost:18080)

## License

This project is licensed under the MIT License.
