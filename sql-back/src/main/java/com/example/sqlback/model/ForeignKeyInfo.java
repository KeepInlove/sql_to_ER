package com.example.sqlback.model;

import lombok.Data;

@Data
public class ForeignKeyInfo {
    private String columnName;
    private String referenceTable;
    private String referenceColumn;
}
