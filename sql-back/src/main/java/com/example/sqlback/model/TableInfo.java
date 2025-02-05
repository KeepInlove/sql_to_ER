package com.example.sqlback.model;

import lombok.Data;
import java.util.List;

@Data
public class TableInfo {
    private String tableName;
    private List<ColumnInfo> columns;
    private List<String> primaryKeys;
    private List<ForeignKeyInfo> foreignKeys;
    private String comment;
}

