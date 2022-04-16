package com.easycode.codegen.sql.core;

import com.easycode.codegen.utils.EnumUtils;
import com.easycode.codegen.sql.core.config.GlobalConfig;
import com.easycode.codegen.sql.core.core.TypeMapping;
import com.easycode.codegen.sql.core.core.meta2entity.IMetaToOrmProcessor;
import com.easycode.codegen.sql.core.core.sql2meta.ISqlToMetaProcessor;
import com.easycode.codegen.sql.core.enumuration.DB;
import com.easycode.codegen.sql.core.enumuration.ORM;
import com.easycode.codegen.sql.core.meta.Table;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * @ClassName: SqlCodegenRunner
 * @Description: sql代码生成
 * @Author: Mr.Zeng
 * @Date: 2021-04-28 13:37
 */
@Slf4j
public class SqlCodegenRunner {

    /**
     * 开始生成代码
     *
     * @param config 全局配置
     */
    public void start(GlobalConfig config) {
        checkConfig(config);
        DB db = EnumUtils.getEnum(DB.class, config.getDbName());
        ORM orm = EnumUtils.getEnum(ORM.class, config.getOrmName());
        String sql = getCreateSqlContent(config.getSqlFilePath());

        Optional.ofNullable(config.getTypeMappings()).ifPresent(TypeMapping.INSTANCE::putAll);

        ISqlToMetaProcessor<?> sqlToMetaProcessor = db.getProcessorCreator().get();
        sqlToMetaProcessor.setConfig(config);
        List<Table> tables = sqlToMetaProcessor.convertTable(sql);
        IMetaToOrmProcessor<?> metaToEntityProcessor = orm.getProcessorCreator().get();
        metaToEntityProcessor.setConfig(config);
        metaToEntityProcessor.generate(tables);
    }

    /**
     * 检查配置是否有误
     *
     * @param config 配置
     */
    private void checkConfig(GlobalConfig config) {
        // 配置检查 TODO
        Objects.requireNonNull(config.getDbName(), "数据库类型不能为空");
        Objects.requireNonNull(config.getOrmName(), "数据库orm框架类型不能为空");
        Objects.requireNonNull(config.getSqlFilePath(), "create sql文件路径不能为空");
        Objects.requireNonNull(config.getSrcJavaPath(), "源码路径不为为空");
        Objects.requireNonNull(config.getBasePackage(), "主要包路径不能为空");
        Objects.requireNonNull(config.getEntityPackageName(), "实体包名不能为空");
        Objects.requireNonNull(config.getEntitySuffix(), "实体类名后缀不能为空");
    }

    /**
     * 获取sql文件内容
     *
     * @param sqlFilePath sql文件路径
     * @return sql建表语句
     */
    @SneakyThrows
    private String getCreateSqlContent(String sqlFilePath) {
        File sqlFile = new File(sqlFilePath);
        log.info("sqlFilePath:" + sqlFile.getAbsolutePath());
        return IOUtils.toString(new FileInputStream(sqlFile), StandardCharsets.UTF_8);
    }

}
