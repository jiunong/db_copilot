package org.sict.db_copilot.service;

import org.sict.db_copilot.config.MultiDbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DbService {

    @Autowired
    private MultiDbProperties multiDbProperties;

    // Map: dbId -> JdbcTemplate
    private final Map<String, JdbcTemplate> jdbcTemplateMap = new ConcurrentHashMap<>();

    private String getDbType(String dbId) {
        if (multiDbProperties.getList() != null) {
            for (MultiDbProperties.DbConfig config : multiDbProperties.getList()) {
                if (config.getId().equals(dbId)) {
                    String driver = config.getDriverClassName();
                    if (driver != null) {
                        String lower = driver.toLowerCase();
                        if (lower.contains("mysql")) return "mysql";
                        if (lower.contains("oracle")) return "oracle";
                        if (lower.contains("dm")) return "dm";
                    }
                    String url = config.getUrl();
                    if (url != null) {
                        String lowerUrl = url.toLowerCase();
                        if (lowerUrl.contains("mysql")) return "mysql";
                        if (lowerUrl.contains(":dm:")) return "dm";
                    }
                }
            }
        }
        return "oracle";
    }

    private String getQuote(String dbId) {
        return "mysql".equals(getDbType(dbId)) ? "`" : "\"";
    }

    @PostConstruct
    public void init() {
        if (multiDbProperties.getList() != null) {
            for (MultiDbProperties.DbConfig config : multiDbProperties.getList()) {
                try {
                    DataSource ds = DataSourceBuilder.create()
                            .url(config.getUrl())
                            .username(config.getUsername())
                            .password(config.getPassword())
                            .driverClassName(config.getDriverClassName())
                            .build();
                    jdbcTemplateMap.put(config.getId(), new JdbcTemplate(ds));
                } catch (Exception e) {
                    System.err.println("Failed to initialize datasource: " + config.getId() + " - " + e.getMessage());
                }
            }
        }
    }

    private JdbcTemplate getJdbcTemplate(String dbId) {
        JdbcTemplate template = jdbcTemplateMap.get(dbId);
        if (template == null) {
            throw new IllegalArgumentException("Database connection not found: " + dbId);
        }
        return template;
    }

    public List<Map<String, String>> getDbList() {
        if (multiDbProperties.getList() == null) return new ArrayList<>();
        return multiDbProperties.getList().stream().map(config -> {
            Map<String, String> map = new HashMap<>();
            map.put("id", config.getId());
            map.put("name", config.getName());
            return map;
        }).collect(Collectors.toList());
    }

    // 获取所有Schema (User)
    public List<Map<String, Object>> getAllSchemas(String dbId) {
        if ("mysql".equals(getDbType(dbId))) {
            String sql = "SELECT schema_name AS username FROM information_schema.schemata ORDER BY schema_name";
            return getJdbcTemplate(dbId).queryForList(sql);
        }
        String sql = "SELECT username FROM all_users ORDER BY username";
        return getJdbcTemplate(dbId).queryForList(sql);
    }

    // 获取指定Schema下的所有表及备注
    public List<Map<String, Object>> getTablesBySchema(String dbId, String schema) {
        if ("mysql".equals(getDbType(dbId))) {
            String sql = "SELECT TABLE_NAME, TABLE_COMMENT AS COMMENTS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME";
            return getJdbcTemplate(dbId).queryForList(sql, schema);
        }
        String sql = "SELECT t.table_name, c.comments " +
                     "FROM all_tables t " +
                     "LEFT JOIN all_tab_comments c ON t.owner = c.owner AND t.table_name = c.table_name " +
                     "WHERE t.owner = ? " +
                     "ORDER BY t.table_name";
        return getJdbcTemplate(dbId).queryForList(sql, schema);
    }

    // 获取表结构详细信息
    public List<Map<String, Object>> getTableColumns(String dbId, String schema, String tableName) {
        if ("mysql".equals(getDbType(dbId))) {
            String sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH AS DATA_LENGTH, NUMERIC_PRECISION AS DATA_PRECISION, NUMERIC_SCALE AS DATA_SCALE, IS_NULLABLE AS NULLABLE, COLUMN_COMMENT AS COMMENTS " +
                         "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
            return getJdbcTemplate(dbId).queryForList(sql, schema, tableName);
        }
        String sql = "SELECT t.column_name, t.data_type, t.data_length, t.data_precision, t.data_scale, t.nullable, c.comments " +
                     "FROM all_tab_columns t " +
                     "LEFT JOIN all_col_comments c ON t.owner = c.owner AND t.table_name = c.table_name AND t.column_name = c.column_name " +
                     "WHERE t.owner = ? AND t.table_name = ? " +
                     "ORDER BY t.column_id";
        return getJdbcTemplate(dbId).queryForList(sql, schema, tableName);
    }

    // 批量全表更新
    public void batchUpdateTableData(String dbId, Map<String, Object> payload) {
        String schema = (String) payload.get("schema");
        String tableName = (String) payload.get("tableName");
        String col = (String) payload.get("columnName");
        String type = (String) payload.get("type");

        if ("shuffle".equals(type)) {
            String sql = String.format(
                    "MERGE INTO \"%s\".\"%s\" T " +
                            "USING (" +
                            "  SELECT T1.rid, T2.val " +
                            "  FROM (" +
                            "      SELECT ROWID as rid, ROW_NUMBER() OVER (ORDER BY ROWID) as rn " +
                            "      FROM \"%s\".\"%s\"" +
                            "  ) T1 " +
                            "  JOIN (" +
                            "      SELECT \"%s\" as val, ROW_NUMBER() OVER (ORDER BY dbms_random.value) as rn " +
                            "      FROM \"%s\".\"%s\"" +
                            "  ) T2 " +
                            "  ON T1.rn = T2.rn" +
                            ") S " +
                            "ON (T.ROWID = S.rid) " +
                            "WHEN MATCHED THEN UPDATE SET T.\"%s\" = S.val",
                    schema, tableName, schema, tableName, col, schema, tableName, col
            );
            getJdbcTemplate(dbId).update(sql);
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE \"").append(schema).append("\".\"").append(tableName).append("\" SET \"").append(col).append("\" = ");

        List<Object> args = new ArrayList<>();

        switch (type) {
            case "fixed":
                sql.append("?");
                args.add(payload.get("value"));
                break;
            case "null":
                sql.append("NULL");
                break;
            case "add":
                sql.append("\"").append(col).append("\" + ?");
                args.add(payload.get("numValue"));
                break;
            case "sub":
                sql.append("\"").append(col).append("\" - ?");
                args.add(payload.get("numValue"));
                break;
            case "mul":
                sql.append("\"").append(col).append("\" * ?");
                args.add(payload.get("numValue"));
                break;
            case "randomStr":
                int len = payload.get("length") instanceof Number ? ((Number) payload.get("length")).intValue() : Integer.parseInt(payload.get("length").toString());
                sql.append("dbms_random.string('x', ?)");
                args.add(len);
                break;
            case "randomCn":
                int lenCn = payload.get("length") instanceof Number ? ((Number) payload.get("length")).intValue() : Integer.parseInt(payload.get("length").toString());
                // Generate a random string of length [lenCn] using a pool of Chinese characters
                // Since dbms_random.string only supports alpha-numeric, we use TRANSLATE to map them to Chinese.
                // We map 'A'..'Z' to common Chinese surnames/characters. It's pseudo-random but sufficient for dummy data.
                sql.append("TRANSLATE(DBMS_RANDOM.STRING('U', ?), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', '赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹')");
                args.add(lenCn);
                break;
            case "randomInt":
                int min = payload.get("min") instanceof Number ? ((Number) payload.get("min")).intValue() : Integer.parseInt(payload.get("min").toString());
                int max = payload.get("max") instanceof Number ? ((Number) payload.get("max")).intValue() : Integer.parseInt(payload.get("max").toString());
                // Oracle dbms_random.value(low, high) returns [low, high)
                sql.append("trunc(dbms_random.value(?, ?))");
                args.add(min);
                args.add(max + 1);
                break;
            case "randomDecimal":
                double minD = payload.get("min") instanceof Number ? ((Number) payload.get("min")).doubleValue() : Double.parseDouble(payload.get("min").toString());
                double maxD = payload.get("max") instanceof Number ? ((Number) payload.get("max")).doubleValue() : Double.parseDouble(payload.get("max").toString());
                // Generate random decimal with 2 decimal places
                sql.append("ROUND(dbms_random.value(?, ?), 2)");
                args.add(minD);
                args.add(maxD);
                break;
            case "mask":
                int s = payload.get("keepStart") instanceof Number ? ((Number) payload.get("keepStart")).intValue() : 0;
                int e = payload.get("keepEnd") instanceof Number ? ((Number) payload.get("keepEnd")).intValue() : 0;
                String c = (String) payload.get("maskChar");
                if (c == null || c.isEmpty()) c = "*";
                
                // SQL Logic:
                // CASE WHEN LENGTH(col) > (s+e) THEN SUBSTR(col, 1, s) || RPAD(c, LENGTH(col)-s-e, c) || SUBSTR(col, -e)
                // ELSE RPAD(c, LENGTH(col), c) END
                // Note: RPAD(str, len, pad) returns str padded to len. 
                // We want a string of mask chars of length X. RPAD('x', X, 'x') works.
                
                sql.append("CASE WHEN LENGTH(\"").append(col).append("\") > ? THEN ");
                args.add(s + e);
                
                sql.append("SUBSTR(\"").append(col).append("\", 1, ?) || ");
                args.add(s);
                
                sql.append("RPAD(?, LENGTH(\"").append(col).append("\") - ?, ?) || ");
                args.add(c);
                args.add(s + e);
                args.add(c);
                
                sql.append("SUBSTR(\"").append(col).append("\", -?) ");
                args.add(e);
                
                sql.append("ELSE RPAD(?, LENGTH(\"").append(col).append("\"), ?) END");
                args.add(c);
                args.add(c);
                break;
            case "replace":
                String find = (String) payload.get("findValue");
                String replace = (String) payload.get("replaceValue");
                boolean isRegex = Boolean.TRUE.equals(payload.get("useRegex"));

                if (isRegex) {
                    // REGEXP_REPLACE(source, pattern, replace, position, occurrence)
                    // occurrence 0 means replace all
                    sql.append("REGEXP_REPLACE(\"").append(col).append("\", ?, ?, 1, 0)");
                    args.add(find);
                    args.add(replace);
                } else {
                    sql.append("REPLACE(\"").append(col).append("\", ?, ?)");
                    args.add(find);
                    args.add(replace);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown batch type: " + type);
        }

        getJdbcTemplate(dbId).update(sql.toString(), args.toArray());
    }

    // 分页查询表数据
    public List<Map<String, Object>> getTableData(String dbId, String schema, String tableName, int page, int size) {
        int startRow = (page - 1) * size;
        
        if ("mysql".equals(getDbType(dbId))) {
             String sql = String.format(
                "SELECT * FROM `%s`.`%s` LIMIT ? OFFSET ?",
                schema, tableName
             );
             return getJdbcTemplate(dbId).queryForList(sql, size, startRow);
        }

        int endRow = page * size;
        
        // 注意：表名和Schema名在SQL拼接时需要防范注入
        // 显式获取 ROWID 用于后续更新定位
        String sql = String.format(
                "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT t.*, ROWIDTOCHAR(t.ROWID) as \"_ROWID_\" FROM \"%s\".\"%s\" t) a WHERE ROWNUM <= ?) WHERE rnum > ?",
                schema, tableName
        );
        return getJdbcTemplate(dbId).queryForList(sql, endRow, startRow);
    }

    // 批量更新表格数据 (基于 ROWID)
    public void updateTableData(String dbId, String schema, String tableName, List<Map<String, Object>> updates) {
        JdbcTemplate jt = getJdbcTemplate(dbId);
        
        // 1. 获取表的所有列类型信息，用于处理特殊类型（如 DATE）
        Map<String, String> columnTypes = new HashMap<>();
        try {
            List<Map<String, Object>> cols = getTableColumns(dbId, schema, tableName);
            for (Map<String, Object> col : cols) {
                // 兼容不同大小写的 Key
                String colName = null;
                String dataType = null;
                
                for (Map.Entry<String, Object> entry : col.entrySet()) {
                    String k = entry.getKey();
                    if ("COLUMN_NAME".equalsIgnoreCase(k)) {
                        colName = (String) entry.getValue();
                    } else if ("DATA_TYPE".equalsIgnoreCase(k)) {
                        dataType = (String) entry.getValue();
                    }
                }
                
                if (colName != null && dataType != null) {
                    columnTypes.put(colName, dataType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Warning: Could not fetch column metadata for update type checking.");
        }

        // 2. 遍历更新
        for (Map<String, Object> update : updates) {
            String rowId = (String) update.get("_ROWID_");
            Map<String, Object> data = (Map<String, Object>) update.get("data");
            
            if (rowId == null || data == null || data.isEmpty()) continue;
            
            StringBuilder sql = new StringBuilder("UPDATE \"" + schema + "\".\"" + tableName + "\" SET ");
            List<Object> params = new ArrayList<>();
            
            boolean first = true;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String colName = entry.getKey();
                Object val = entry.getValue();

                if (!first) sql.append(", ");
                sql.append("\"").append(colName).append("\" = ?");
                
                // 处理特殊类型转换，避免 ORA-01861
                // 尝试查找类型时，如果不匹配，也尝试大小写无关查找
                String type = columnTypes.get(colName);
                if (type == null) {
                    for (Map.Entry<String, String> tEntry : columnTypes.entrySet()) {
                        if (tEntry.getKey().equalsIgnoreCase(colName)) {
                            type = tEntry.getValue();
                            break;
                        }
                    }
                }
                
                params.add(convertValue(val, type));
                
                first = false;
            }
            
            sql.append(" WHERE ROWID = CHARTOROWID(?)");
            params.add(rowId);
            
            jt.update(sql.toString(), params.toArray());
        }
    }

    private Object convertValue(Object value, String dataType) {
        if (value == null || dataType == null) {
            return value;
        }
        if (value instanceof String) {
            String strVal = (String) value;
            String upperType = dataType.toUpperCase();
            if (upperType.contains("DATE") || upperType.contains("TIMESTAMP")) {
                // 尝试解析常见日期格式
                Object parsedDate = parseDate(strVal);
                if (parsedDate != null) return parsedDate;
            }
        }
        return value;
    }

    private Object parseDate(String dateStr) {
        // 先尝试直接转 Timestamp (JDBC 格式)
        try {
            return Timestamp.valueOf(dateStr);
        } catch (Exception ignored) {}
        
        // 支持几种常见格式，根据需要扩展
        String[] patterns = {
            "yyyy-MM-dd HH:mm:ss.S", // Java/Oracle optional fractional seconds
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX", // ISO
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                if (pattern.contains("T") || dateStr.contains("T")) {
                     return Timestamp.valueOf(LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern)));
                } else {
                     if (pattern.equals("yyyy-MM-dd")) {
                         return java.sql.Date.valueOf(dateStr);
                     }
                     // Use SimpleDateFormat logic or similar for leniency? No, stay strict but try varied patterns
                     return Timestamp.valueOf(LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern)));
                }
            } catch (Exception ignored) {
            }
        }
        return dateStr; // Fallback
    }

    // 获取表总记录数
    public Long getTableCount(String dbId, String schema, String tableName) {
        String quote = getQuote(dbId);
        String sql = String.format("SELECT COUNT(1) FROM %s%s%s.%s%s%s", quote, schema, quote, quote, tableName, quote);
        return getJdbcTemplate(dbId).queryForObject(sql, Long.class);
    }

    // 执行自定义SQL
    public List<Map<String, Object>> executeSql(String dbId, String sql) {
        // 简单判断是查询还是更新 (实际场景需要更严谨的处理)
        sql = sql.trim();
        if (sql.toUpperCase().startsWith("SELECT")) {
            return getJdbcTemplate(dbId).queryForList(sql);
        } else {
            int rows = getJdbcTemplate(dbId).update(sql);
            return java.util.Collections.singletonList(
                java.util.Collections.singletonMap("AFFECTED_ROWS", rows)
            );
        }
    }

    // 更新表备注
    public void commentOnTable(String dbId, String schema, String tableName, String comment) {
        String safeComment = comment == null ? "" : comment.replace("'", "''");
        if ("mysql".equals(getDbType(dbId))) {
             String sql = String.format("ALTER TABLE `%s`.`%s` COMMENT = '%s'", schema, tableName, safeComment);
             getJdbcTemplate(dbId).execute(sql);
             return;
        }
        String sql = String.format("COMMENT ON TABLE \"%s\".\"%s\" IS '%s'", schema, tableName, safeComment);
        getJdbcTemplate(dbId).execute(sql);
    }

    // 更新字段备注
    public void commentOnColumn(String dbId, String schema, String tableName, String columnName, String comment) {
        if ("mysql".equals(getDbType(dbId))) {
            return;
        }
        String safeComment = comment == null ? "" : comment.replace("'", "''");
        String sql = String.format("COMMENT ON COLUMN \"%s\".\"%s\".\"%s\" IS '%s'", schema, tableName, columnName, safeComment);
        getJdbcTemplate(dbId).execute(sql);
    }

    // 检查数据库连接状态
    public boolean checkConnection(String dbId) {
        try {
            JdbcTemplate template = jdbcTemplateMap.get(dbId);
            if (template == null) return false;
            DataSource ds = template.getDataSource();
            if (ds == null) return false;
            
            // 尝试获取连接并验证，超时 2 秒
            try (java.sql.Connection conn = ds.getConnection()) {
                return conn.isValid(2);
            }
        } catch (Exception e) {
            // 连接失败
            return false;
        }
    }
}
