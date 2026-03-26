package org.sict.db_copilot.controller;

import org.sict.db_copilot.config.MultiDbProperties;
import org.sict.db_copilot.service.DbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
public class DbController {

    @Autowired
    private DbService dbService;

    @GetMapping("/list")
    public List<Map<String, String>> getDbList() {
        return dbService.getDbList();
    }

    @GetMapping("/{dbId}/schemas")
    public List<Map<String, Object>> getSchemas(@PathVariable String dbId) {
        return dbService.getAllSchemas(dbId);
    }

    @GetMapping("/{dbId}/tables/{schema}")
    public List<Map<String, Object>> getTables(@PathVariable String dbId, @PathVariable String schema) {
        return dbService.getTablesBySchema(dbId, schema);
    }

    @GetMapping("/{dbId}/columns/{schema}/{tableName}")
    public List<Map<String, Object>> getColumns(@PathVariable String dbId, @PathVariable String schema, @PathVariable String tableName) {
        return dbService.getTableColumns(dbId, schema, tableName);
    }

    @GetMapping("/{dbId}/data/{schema}/{tableName}")
    public Map<String, Object> getData(@PathVariable String dbId,
                                       @PathVariable String schema,
                                       @PathVariable String tableName,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        List<Map<String, Object>> data = dbService.getTableData(dbId, schema, tableName, page, size);
        Long total = dbService.getTableCount(dbId, schema, tableName);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("data", data);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @PostMapping("/{dbId}/data/{schema}/{tableName}/query")
    public Map<String, Object> queryData(@PathVariable String dbId,
                                       @PathVariable String schema,
                                       @PathVariable String tableName,
                                       @RequestBody Map<String, Object> body) {
        int page = body.get("page") instanceof Number ? ((Number) body.get("page")).intValue() : 1;
        int size = body.get("size") instanceof Number ? ((Number) body.get("size")).intValue() : 10;
        String sortColumn = (String) body.get("sortColumn");
        String sortOrder = (String) body.get("sortOrder");
        List<Map<String, Object>> filters = (List<Map<String, Object>>) body.get("filters");

        List<Map<String, Object>> data = dbService.getTableData(dbId, schema, tableName, page, size, sortColumn, sortOrder, filters);
        Long total = dbService.getTableCount(dbId, schema, tableName, filters);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("data", data);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @PostMapping("/{dbId}/data/update")
    public void updateData(@PathVariable String dbId, @RequestBody Map<String, Object> payload) {
        String schema = (String) payload.get("schema");
        String tableName = (String) payload.get("tableName");
        List<Map<String, Object>> updates = (List<Map<String, Object>>) payload.get("updates");
        dbService.updateTableData(dbId, schema, tableName, updates);
    }

    @PostMapping("/{dbId}/data/batch-update")
    public void batchUpdateData(@PathVariable String dbId, @RequestBody Map<String, Object> payload) {
        dbService.batchUpdateTableData(dbId, payload);
    }

    @PostMapping("/{dbId}/execute")
    public Object executeSql(@PathVariable String dbId, @RequestBody Map<String, String> payload) {
        String sql = payload.get("sql");
        try {
            return dbService.executeSql(dbId, sql);
        } catch (Exception e) {
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    @PostMapping("/add")
    public void addDb(@RequestBody MultiDbProperties.DbConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId("db-" + System.currentTimeMillis());
        }
        dbService.addDbConfig(config);
    }

    @PostMapping("/update")
    public void updateDb(@RequestBody MultiDbProperties.DbConfig config) {
        dbService.updateDbConfig(config);
    }

    @PostMapping("/delete/{id}")
    public void deleteDb(@PathVariable String id) {
        dbService.deleteDbConfig(id);
    }

    @GetMapping("/config/{id}")
    public MultiDbProperties.DbConfig getDbConfig(@PathVariable String id) {
        return dbService.getDbConfig(id);
    }

    @PostMapping("/{dbId}/comment/table")
    public void commentTable(@PathVariable String dbId, @RequestBody Map<String, String> payload) {
        dbService.commentOnTable(dbId, payload.get("schema"), payload.get("tableName"), payload.get("comment"));
    }

    @PostMapping("/{dbId}/comment/column")
    public void commentColumn(@PathVariable String dbId, @RequestBody Map<String, String> payload) {
        dbService.commentOnColumn(dbId, payload.get("schema"), payload.get("tableName"), payload.get("columnName"), payload.get("comment"));
    }

    @GetMapping("/{dbId}/status")
    public Map<String, Object> getDbStatus(@PathVariable String dbId) {
        boolean status = dbService.checkConnection(dbId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", dbId);
        result.put("status", status ? "UP" : "DOWN");
        return result;
    }
}
