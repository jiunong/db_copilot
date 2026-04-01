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

**Option A: Standard Network Mode (Recommended for remote databases)**

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

**Option B: Host Network Mode (Recommended for local databases)**

If you need to access local databases on the same server, use host network mode:

```bash
docker run -d \
  --name db-copilot \
  --network host \
  -v /opt/db-copilot/config:/app/config \
  -v /opt/db-copilot/logs:/app/logs \
  -e JAVA_OPTS="-Xms512m -Xmx1024m" \
  db-copilot:v1
```

Note: When using host network mode, the application will be accessible on port 18080 directly without port mapping, and can access local databases using `localhost` or `127.0.0.1`.

- Access the application at: `http://<your-server-ip>:18080`
- Database configurations will be saved to `/opt/db-copilot/config/db-copilot-config.json` (persisted via volume mount).
- Logs will be available in `/opt/db-copilot/logs`.
- The `db-copilot-config.json` file is fully editable and all changes are automatically persisted.

### Configuration Persistence

The application uses `db-copilot-config.json` to store database connection configurations. This file:

- **Auto-detected**: The application automatically checks for the `config/` directory and uses `/app/config/db-copilot-config.json` when running in Docker.
- **Fully Editable**: You can manually edit this file to add, modify, or remove database connections.
- **Auto-saved**: Any changes made through the web UI are automatically saved to this file.
- **Persistent**: Thanks to Docker volume mounts, your configurations survive container restarts and updates.

#### Configuration File Format

```json
[
  {
    "id": "LocalMySql",
    "name": "Local MySQL",
    "url": "jdbc:mysql://localhost:3306/mysql?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
    "username": "root",
    "password": "password",
    "driverClassName": "com.mysql.cj.jdbc.Driver"
  },
  {
    "id": "LocalOracle",
    "name": "Local Oracle",
    "url": "jdbc:oracle:thin:@//localhost:1521/ORCL",
    "username": "system",
    "password": "password",
    "driverClassName": "oracle.jdbc.OracleDriver"
  }
]
```

**Important:** For Docker deployments, replace `localhost` with:
- `host.docker.internal` (Docker Desktop on Mac/Windows)
- `172.17.0.1` (Linux Docker bridge IP)
- Or use `--network host` mode (recommended for local databases)

#### Manual Configuration

To manually add or edit database connections:

1. Stop the container (if running):
   ```bash
   docker stop db-copilot
   ```

2. Edit the configuration file:
   ```bash
   vi /opt/db-copilot/config/db-copilot-config.json
   ```

3. Start the container:
   ```bash
   docker start db-copilot
   ```

The application will automatically load the updated configuration on startup.

### Troubleshooting Docker Network Issues

#### Cannot Connect to Local Database

**Symptom:** Connection refused or timeout when trying to connect to localhost databases

**Solution:**
1. **Use host network mode** (recommended):
   ```bash
   docker stop db-copilot
   docker rm db-copilot
   docker run -d \
     --name db-copilot \
     --network host \
     -v /opt/db-copilot/config:/app/config \
     -v /opt/db-copilot/logs:/app/logs \
     -e JAVA_OPTS="-Xms512m -Xmx1024m" \
     db-copilot:v1
   ```

2. **Or update database URL** to use host IP:
   - Get docker bridge IP: `ip addr show docker0 | grep inet | awk '{print $2}' | cut -d/ -f1`
   - Update config: Replace `localhost` with the bridge IP (e.g., `172.17.0.1`)

#### Test Database Connectivity from Container

**Test connection from inside container:**
```bash
# Enter container
docker exec -it db-copilot sh

# Test Oracle connection
nc -zv localhost 1521

# Test MySQL connection
nc -zv localhost 3306

# Exit container
exit
```

#### Check Database Listener Status

**Oracle:**
```bash
# Check if listener is running
lsnrctl status

# Check listener configuration
cat $ORACLE_HOME/network/admin/listener.ora
```

**MySQL:**
```bash
# Check MySQL service
systemctl status mysqld

# Check MySQL configuration
cat /etc/my.cnf | grep bind-address
```

### Docker Network Configuration for Local Database Access

When running DbCopilot in Docker, accessing local databases on the host machine requires special network configuration due to Docker's network isolation.

#### Problem
Docker containers run in an isolated network namespace, so `localhost` or `127.0.0.1` inside the container refers to the container itself, not the host machine.

#### Solutions

**1. Use Host Network Mode (Recommended for local databases)**

```bash
docker run -d \
  --name db-copilot \
  --network host \
  -v /opt/db-copilot/config:/app/config \
  -v /opt/db-copilot/logs:/app/logs \
  -e JAVA_OPTS="-Xms512m -Xmx1024m" \
  db-copilot:v1
```

**Benefits:**
- Container shares host's network stack
- Can access local databases using `localhost` or `127.0.0.1`
- No port mapping needed
- Simplest configuration

**Configuration Example:**
```json
{
  "id": "LocalOracle",
  "name": "Local Oracle",
  "url": "jdbc:oracle:thin:@//localhost:1521/ORCL",
  "username": "your_username",
  "password": "your_password",
  "driverClassName": "oracle.jdbc.OracleDriver"
}
```

**2. Use `host.docker.internal` (Docker Desktop only)**

If using Docker Desktop (Mac/Windows), use the special DNS name:

```json
{
  "id": "LocalOracle",
  "name": "Local Oracle",
  "url": "jdbc:oracle:thin:@//host.docker.internal:1521/ORCL",
  "username": "your_username",
  "password": "your_password",
  "driverClassName": "oracle.jdbc.OracleDriver"
}
```

**3. Use Host's Bridge IP (Linux Docker)**

Get the Docker bridge IP address:

```bash
# Get docker bridge IP
ip addr show docker0 | grep inet | awk '{print $2}' | cut -d/ -f1
# Typically: 172.17.0.1
```

Then use this IP in your configuration:

```json
{
  "id": "LocalOracle",
  "name": "Local Oracle",
  "url": "jdbc:oracle:thin:@//172.17.0.1:1521/ORCL",
  "username": "your_username",
  "password": "your_password",
  "driverClassName": "oracle.jdbc.OracleDriver"
}
```

**4. Use Host's Actual Network IP**

Get the host's actual network interface IP:

```bash
# Get primary network interface IP
hostname -I | awk '{print $1}'
```

Use this IP in your database configuration.

#### Firewall Configuration

Ensure your firewall allows connections from the container to the database:

```bash
# For Oracle (port 1521)
sudo firewall-cmd --add-port=1521/tcp --permanent
sudo firewall-cmd --reload

# For MySQL (port 3306)
sudo firewall-cmd --add-port=3306/tcp --permanent
sudo firewall-cmd --reload

# Or allow from Docker network
sudo firewall-cmd --zone=trusted --add-source=172.17.0.0/16 --permanent
sudo firewall-cmd --reload
```

#### Database Listener Configuration

Ensure your database listener is configured to accept connections from the Docker network:

**Oracle: Check listener.ora**
```bash
# Listener should listen on 0.0.0.0 or specific interface, not just 127.0.0.1
LISTENER =
  (DESCRIPTION_LIST =
    (DESCRIPTION =
      (ADDRESS = (PROTOCOL = TCP)(HOST = 0.0.0.0)(PORT = 1521))
    )
  )
```

**MySQL: Check my.cnf**
```bash
# Bind to all interfaces
bind-address = 0.0.0.0
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
