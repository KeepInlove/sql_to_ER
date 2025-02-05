package com.example.sqlback.model;

import lombok.Data;

@Data
public class ColumnInfo {
    private String name;
    private String type;
    private boolean nullable;
    private String defaultValue;
    private String comment;
}
