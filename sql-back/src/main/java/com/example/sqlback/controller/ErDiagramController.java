package com.example.sqlback.controller;

import com.example.sqlback.model.TableInfo;
import com.example.sqlback.service.SqlParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/er-diagram")
@RequiredArgsConstructor
public class ErDiagramController {

    private final SqlParserService sqlParserService;

    @PostMapping("/parse")
    public ResponseEntity<List<TableInfo>> parseSql(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<TableInfo> tables = sqlParserService.parseSql(sql);
        return ResponseEntity.ok(tables);
    }
} 