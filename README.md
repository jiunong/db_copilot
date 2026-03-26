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

## Deployment

### 1. Build Local Package

Package the application using Maven:

```bash
mvn clean package -DskipTests
```

This will generate `db_copilot-0.0.1-SNAPSHOT.jar` in the `target/` directory.

### 2. Deploy to Linux Server with Docker

#### Step 1: Upload Files

Create a directory on your server (e.g., `/opt/db-copilot`) and upload the following files:
1. `target/db_copilot-0.0.1-SNAPSHOT.jar`
2. `Dockerfile`

#### Step 2: Build Docker Image

Run the following command in the directory where you uploaded the files:

```bash
docker build -t db-copilot:v1 .
```

#### Step 3: Prepare Data Directory

Create a directory to store persistent configurations (database connections):

```bash
mkdir -p /opt/db-copilot/config
mkdir -p /opt/db-copilot/logs
```

#### Step 4: Run Container

Run the application mapping the config directory to persist your database settings:

```bash
docker run -d \
  --name db-copilot \
  -p 18080:18080 \
  -v /opt/db-copilot/config:/app/config \
  -v /opt/db-copilot/logs:/app/logs \
  -e JAVA_OPTS="-Xms512m -Xmx1024m" \
  db-copilot:v1
```

- Access the application at: `http://<your-server-ip>:18080`
- Database configurations will be saved to `/opt/db-copilot/config/db-copilot-config.json`.
- Logs will be available in `/opt/db-copilot/logs`.

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
