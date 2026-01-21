# DbCopilot

A lightweight, web-based Oracle database management tool built with Spring Boot and Vue.js.

## Features

- **Multi-Database Support**: Configure and manage connections to multiple Oracle databases.
- **Schema & Table Browser**: Easily browse schemas and tables with support for filtering by name or comment.
- **Data Editor**:
  - View table data with pagination.
  - Inline editing and saving of records.
  - **Batch Operations**: powerful tools to update multiple rows at once (Fixed value, Random data, Sequence, Math operations, Data Masking/Desensitization).
- **Structure Viewer**: Inspect table columns, data types, and nullability.
- **Metadata Management**: View and edit Table and Column comments directly from the UI.
- **SQL Editor**: Execute raw SQL queries and view results.
- **Frontend**: Responsive UI built with Vue.js and Element UI.

## Technology Stack

- **Backend**: Java 8, Spring Boot 2.6.13
- **Frontend**: Vue.js 2, Element UI, Axios
- **Database**: Oracle (ojdbc8)

## Getting Started

### Prerequisites

- JDK 1.8
- Maven 3.x

### Configuration

Configure your database connections in `src/main/resources/application.properties`. You can define multiple data sources using the `custom.datasource.list` prefix.

```properties
server.port=8080

# Example Configuration
custom.datasource.list[0].id=LocalOracle
custom.datasource.list[0].name=Local Dev
custom.datasource.list[0].url=jdbc:oracle:thin:@//localhost:1521/ORCL
custom.datasource.list[0].username=scott
custom.datasource.list[0].password=tiger
custom.datasource.list[0].driver-class-name=oracle.jdbc.OracleDriver

custom.datasource.list[1].id=Prod
custom.datasource.list[1].name=Production DB
custom.datasource.list[1].url=jdbc:oracle:thin:@//192.168.1.100:1521/ORCL
...
```

### Building and Running

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Run the application:**
   ```bash
   java -jar target/db_copliot-0.0.1-SNAPSHOT.jar
   ```
   Or run directly with Maven:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the Web UI:**
   Open your browser and navigate to:
   [http://localhost:8080](http://localhost:8080)

## License

This project is licensed under the MIT License.
