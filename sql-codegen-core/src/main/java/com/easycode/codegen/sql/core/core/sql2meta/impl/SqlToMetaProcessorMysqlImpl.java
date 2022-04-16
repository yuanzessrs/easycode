package com.easycode.codegen.sql.core.core.sql2meta.impl;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLDataTypeImpl;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.easycode.codegen.sql.core.core.sql2meta.ISqlToMetaProcessor;
import com.easycode.codegen.sql.core.core.sql2meta.context.MysqlContext;
import com.easycode.codegen.sql.core.meta.Column;
import com.easycode.codegen.sql.core.meta.Table;
import com.easycode.codegen.utils.FormatUtils;
import com.easycode.codegen.sql.core.config.GlobalConfig;
import com.easycode.codegen.sql.core.core.TypeMapping;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @ClassName: SqlToMetaProcessorMysqlImpl
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-27 22:55
 */
public class SqlToMetaProcessorMysqlImpl implements ISqlToMetaProcessor<MysqlContext> {

    private GlobalConfig config = null;

    @Override
    public void setConfig(GlobalConfig config) {
        this.config = config;
    }

    @Override
    public void setContext(MysqlContext mysqlContext) {

    }

    @Override
    public List<Table> convertTable(String createTableSql) {
        return SQLUtils.parseStatements(createTableSql, DbType.mysql).stream()
                .filter(o -> o instanceof MySqlCreateTableStatement)
                .map(o -> (MySqlCreateTableStatement) o)
                .map(tableStat -> {
                    Table table = new Table();
                    table.setTableName(FormatUtils.escapeQuotes(tableStat.getTableName()));
                    table.setClassName(
                            FormatUtils.snakeToUpperCamel(table.getTableName()) + "Autogen" + config.getEntitySuffix()
                    );
                    Optional.ofNullable(tableStat.getComment())
                            .ifPresent(comment -> table.setTableComment(FormatUtils.escapeQuotes(comment.toString())));
                    String pkName = tableStat.getPrimaryKeyNames()
                            .stream()
                            .limit(1)
                            // 去掉 `
                            .map(FormatUtils::escapeQuotes)
                            .findFirst()
                            .orElseThrow(
                                    () -> new RuntimeException(String.format("当前表不存在主键:%s", table.getTableName())));

                    if (tableStat.getPrimaryKeyNames().size() > 1) {
                        throw new RuntimeException("暂不支持联合主键");
                    }

                    table.setFieldMappings(tableStat.getColumnDefinitions().stream().map(columnDef -> {
                        Table.FiledMapping mapping = new Table.FiledMapping();
                        mapping.setKey(FormatUtils.smallSnakeToUpperSnake(columnDef.getColumnName()));
                        mapping.setValue(FormatUtils.escapeQuotes(columnDef.getColumnName()));
                        return mapping;
                    }).collect(Collectors.toList()));

                    table.setColumns(tableStat.getColumnDefinitions().stream().map(columnDef -> {
                        SQLDataTypeImpl dataType = (SQLDataTypeImpl) columnDef.getDataType();

                        Column column = new Column();
                        column.setColumnName(FormatUtils.escapeQuotes(columnDef.getColumnName()));

                        column.setIsPrimaryKey(pkName.equals(column.getColumnName()));
                        column.setIsAutoIncrement(columnDef.isAutoIncrement());

                        column.setFieldName(FormatUtils.snakeToLowerCamel(columnDef.getColumnName()));

                        Optional.ofNullable(columnDef.getComment())
                                .ifPresent(comment -> column.setComment(FormatUtils.escapeQuotes(comment.toString())));

                        column.setDbType(dataType.getName());
                        column.setDbTypeDesc(dataType.toString());

                        column.setJavaType(dbTypeToJavaType(dataType.getName(), dataType.isUnsigned()));

                        return column;
                    }).collect(Collectors.toList()));
                    return table;
                }).collect(Collectors.toList());
    }

    @Override
    public String dbTypeToJavaType(String dbType, boolean isUnsigned) {
        return TypeMapping.INSTANCE.getOrDefault(isUnsigned ? dbType + "@unsigned" : dbType, dbType);
    }
}
