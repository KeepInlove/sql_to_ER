package com.example.sqlback.service;

import com.example.sqlback.model.TableInfo;
import com.example.sqlback.model.ColumnInfo;
import com.example.sqlback.model.ForeignKeyInfo;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SqlParserService {

    public List<TableInfo> parseSql(String sql) {
        List<TableInfo> tables = new ArrayList<>();
        try {
            log.info("开始解析SQL: {}", sql);

            // 预处理 SQL，移除 UNIQUE INDEX 语句
            String processedSql = preprocessSql(sql);

            // 解析多个SQL语句
            Statements statements = CCJSqlParserUtil.parseStatements(processedSql);
            log.info("成功解析SQL语句，共有{}条语句", statements.getStatements().size());

            for (Statement statement : statements.getStatements()) {
                log.debug("处理语句: {}", statement);
                if (statement instanceof CreateTable) {
                    CreateTable createTable = (CreateTable) statement;
                    log.info("解析CREATE TABLE语句: {}", createTable.getTable().getName());
                    TableInfo tableInfo = parseCreateTable(createTable);
                    tables.add(tableInfo);
                }
            }

            log.info("SQL解析完成，共解析出{}个表", tables.size());
            return tables;
        } catch (JSQLParserException e) {
            log.error("SQL解析错误: {}", e.getMessage());
            throw new RuntimeException("SQL语法错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理SQL时发生意外错误", e);
            throw new RuntimeException("处理SQL时发生错误: " + e.getMessage());
        }
    }

    private String preprocessSql(String sql) {
        // 使用正则表达式移除 UNIQUE INDEX 语句
        String[] lines = sql.split("\n");
        StringBuilder processedSql = new StringBuilder();

        for (String line : lines) {
            // 跳过包含 UNIQUE INDEX 的行
            if (!line.trim().toUpperCase().contains("UNIQUE INDEX")) {
                processedSql.append(line).append("\n");
            }
        }

        return processedSql.toString();
    }

    private TableInfo parseCreateTable(CreateTable createTable) {
        TableInfo tableInfo = new TableInfo();
        String tableName = createTable.getTable().getName();
        tableInfo.setTableName(tableName);

        List<ColumnInfo> columns = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        List<ForeignKeyInfo> foreignKeys = new ArrayList<>();

        // 解析列定义
        log.debug("开始解析表{}的列定义", tableName);
        for (ColumnDefinition col : createTable.getColumnDefinitions()) {
            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setName(col.getColumnName());
            columnInfo.setType(col.getColDataType().getDataType());
            columnInfo.setNullable(true); // 默认可为空

            // 提取列注释
            String columnComment = col.getColumnSpecs().stream()
                .filter(spec -> spec.toUpperCase().contains("COMMENT"))
                .map(spec -> {
                    // 添加调试日志
                    log.debug("列规格列表: {}", col.getColumnSpecs());
                    int commentIndex = col.getColumnSpecs().indexOf(spec);
                    if (commentIndex >= 0 && commentIndex + 1 < col.getColumnSpecs().size()) {
                        String comment = col.getColumnSpecs().get(commentIndex + 1);
                        log.debug("找到注释内容: [{}]", comment);
                        // 去除单引号
                        return comment.replaceAll("^'|'$", "");
                    }
                    return col.getColumnName();
                })
                .findFirst().orElse(col.getColumnName());
            columnInfo.setComment(columnComment);
            log.info("列{}的注释: {}", col.getColumnName(), columnInfo.getComment());

            // 检查列约束
            if (col.getColumnSpecs() != null) {
                String columnSpecs = String.join(" ", col.getColumnSpecs()).toUpperCase();
                if (columnSpecs.contains("PRIMARY KEY")) {
                    primaryKeys.add(col.getColumnName());
                    log.info("找到主键: {}.{}", tableName, col.getColumnName());
                }
                if (columnSpecs.contains("NOT NULL")) {
                    columnInfo.setNullable(false);
                }
            }

            columns.add(columnInfo);
        }

        // 提取表注释
        String tableComment = createTable.getTableOptionsStrings().stream()
                .filter(option -> option.equalsIgnoreCase("COMMENT"))
                .findFirst()
                .map(commentKey -> {
                    int commentIndex = createTable.getTableOptionsStrings().indexOf(commentKey);
                    List<String> options = createTable.getTableOptionsStrings();

                    // 处理两种格式：
                    // 1. COMMENT '内容'
                    // 2. COMMENT = '内容'
                    if (commentIndex + 1 < options.size()) {
                        String nextToken = options.get(commentIndex + 1);

                        // 格式1: 直接跟随注释内容
                        if (nextToken.startsWith("'")) {
                            String fullComment = String.join(" ", options.subList(commentIndex + 1, options.size()));
                            return parseQuotedString(fullComment);
                        }

                        // 格式2: 包含等号
                        if (nextToken.equals("=") && commentIndex + 2 < options.size()) {
                            String commentValue = options.get(commentIndex + 2);
                            return parseQuotedString(commentValue);
                        }
                    }

                    log.warn("无法解析表注释格式: {}", options);
                    return tableName;
                })
                .orElse(tableName);

        tableInfo.setComment(tableComment);
        log.info("表{}的注释: {}", tableName, tableInfo.getComment());



        // 解析外键约束
        if (createTable.getIndexes() != null) {
            log.debug("开始解析表{}的外键约束", tableName);
            for (var index : createTable.getIndexes()) {
                if (index instanceof ForeignKeyIndex) {
                    ForeignKeyIndex fk = (ForeignKeyIndex) index;
                    ForeignKeyInfo fkInfo = new ForeignKeyInfo();
                    fkInfo.setColumnName(fk.getColumns().get(0).getColumnName());
                    fkInfo.setReferenceTable(fk.getTable().getName());
                    fkInfo.setReferenceColumn(fk.getReferencedColumnNames().get(0));
                    foreignKeys.add(fkInfo);
                    log.info("找到外键: {}.{} -> {}.{}",
                        tableName, fkInfo.getColumnName(),
                        fkInfo.getReferenceTable(), fkInfo.getReferenceColumn());
                }
            }
        }

        tableInfo.setColumns(columns);
        tableInfo.setPrimaryKeys(primaryKeys);
        tableInfo.setForeignKeys(foreignKeys);

        log.info("表{}解析完成: {}列, {}个主键, {}个外键",
            tableName, columns.size(), primaryKeys.size(), foreignKeys.size());

        return tableInfo;
    }
    // 辅助方法：解析带引号的字符串
    private String parseQuotedString(String input) {
        // 匹配单引号或双引号包裹的内容
        Pattern pattern = Pattern.compile("^(['\"])(.*)\\1$");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(2);
        }
        // 如果未匹配到完整引号，尝试直接处理
        return input.replaceAll("^['\"]|['\"]$", "");
    }
}
