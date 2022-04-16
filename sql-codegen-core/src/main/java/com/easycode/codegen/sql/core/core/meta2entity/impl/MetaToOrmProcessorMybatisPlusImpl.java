package com.easycode.codegen.sql.core.core.meta2entity.impl;

import com.easycode.codegen.utils.VelocityUtils;
import com.easycode.codegen.sql.core.config.GlobalConfig;
import com.easycode.codegen.sql.core.config.MybatisPlusConfig;
import com.easycode.codegen.sql.core.config.MybatisPlusConfig.LogicDeleteColumn;
import com.easycode.codegen.sql.core.core.meta2entity.IMetaToOrmProcessor;
import com.easycode.codegen.sql.core.core.meta2entity.context.MybatisPlusContext;
import com.easycode.codegen.sql.core.meta.Table;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;

/**
 * @ClassName: MetaToEntityProcessorMybatisPlusImpl
 * @Description: 生成MybatisPlus文件
 * @Author: Mr.Zeng
 * @Date: 2021-04-27 22:37
 */
public class MetaToOrmProcessorMybatisPlusImpl implements IMetaToOrmProcessor<MybatisPlusContext> {

    private GlobalConfig config = null;

    @Override
    public void setConfig(GlobalConfig config) {
        this.config = config;
    }

    @Override
    public void setContext(MybatisPlusContext mybatisPlusContext) {

    }

    @Override
    public void generate(List<Table> tables) {
        tables.forEach(table -> {
            processLogicalDeleteField(table);
            processAutoFillFiled(table);
            processImports(table);
            Map<String, Object> params = new HashMap<>(8);
            params.put("entity", table);
            params.put("config", config);
            File file = new File(config.getEntityDirPath() + table.getClassName() + ".java");
            VelocityUtils.render("template/mybatis_plus_entity.vm", params, file);
        });
    }

    private void processImports(Table table) {
        List<String> imports = new ArrayList<>();
        imports.add("com.baomidou.mybatisplus.annotation.IdType");
        imports.add("com.baomidou.mybatisplus.annotation.TableId");
        imports.add("com.baomidou.mybatisplus.annotation.TableName");
        imports.add("com.baomidou.mybatisplus.annotation.TableField");
        table.getColumns().forEach(column -> {
            if (Boolean.TRUE.equals(column.getIsLogicalDeleteField())) {
                imports.add("com.baomidou.mybatisplus.annotation.TableLogic");
            }
            if (Boolean.TRUE.equals(column.getIsAutoFillWhenInsert())
                    || Boolean.TRUE.equals(column.getIsAutoFillWhenUpdate())
                    || Boolean.TRUE.equals(column.getIsAutoFillWhenInsertOrUpdate())) {
                imports.add("com.baomidou.mybatisplus.annotation.FieldFill");
            }
            if (!ObjectUtils.isEmpty(column.getImport())) {
                imports.add(column.getImport());
            }
        });
        table.setImports(imports.stream().distinct().collect(Collectors.toList()));
    }

    /**
     * 处理逻辑删除字段
     *
     * @param table 表信息
     */
    private void processLogicalDeleteField(Table table) {
        Map<String, LogicDeleteColumn> logicDelColMap = Optional.ofNullable(config.getMybatisPlusConfig())
                .map(MybatisPlusConfig::getLogicDelCols)
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(LogicDeleteColumn::getColumnName, Function.identity()));
        table.getColumns().forEach(column -> {
            if (logicDelColMap.containsKey(column.getColumnName())) {
                LogicDeleteColumn logicDeleteColumn = logicDelColMap.get(column.getColumnName());
                column.setIsLogicalDeleteField(true);
                column.setLogicalDeletedValue(logicDeleteColumn.getDeletedValue());
                column.setLogicalNotDeletedValue(logicDeleteColumn.getNotDeletedValue());
            }
        });
    }

    /**
     * 处理自动填充字段
     *
     * @param table 表信息
     */
    public void processAutoFillFiled(Table table) {
        Optional.ofNullable(config).map(GlobalConfig::getMybatisPlusConfig).ifPresent(mybatisConfig -> {
            String tableName = table.getTableName();
            Set<String> autoInsertFields = Optional
                    .ofNullable(mybatisConfig.getAutoInsertFields())
                    .orElse(Collections.emptySet());

            Set<String> autoUpdateFields = Optional
                    .ofNullable(mybatisConfig.getAutoUpdateFields())
                    .orElse(Collections.emptySet());

            Set<String> autoInsertOrUpdateFields = Optional
                    .ofNullable(mybatisConfig.getAutoInsertOrUpdateFields())
                    .orElse(Collections.emptySet());

            table.getColumns().forEach(column -> {
                String colName = column.getColumnName();
                String uniqueColName = tableName + "@" + column.getColumnName();
                if (autoInsertFields.contains(colName) || autoInsertFields.contains(uniqueColName)) {
                    column.setIsAutoFillWhenInsert(true);
                } else if (autoUpdateFields.contains(colName) || autoUpdateFields.contains(uniqueColName)) {
                    column.setIsAutoFillWhenUpdate(true);
                } else if (autoInsertOrUpdateFields.contains(colName)
                        || autoInsertOrUpdateFields.contains(uniqueColName)) {
                    column.setIsAutoFillWhenInsertOrUpdate(true);
                }
            });
        });
    }
}
