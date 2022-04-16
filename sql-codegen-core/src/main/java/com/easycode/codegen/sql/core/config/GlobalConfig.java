package com.easycode.codegen.sql.core.config;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: GlobalConfig
 * @Description: 实体生成全局配置
 * @Author: Mr.Zeng
 * @Date: 2021-04-17 20:18
 */
@Data
@Slf4j
public class GlobalConfig {

    /**
     * 数据库类型
     */
    private String dbName;
    /**
     * orm框架类型
     */
    private String ormName;
    /**
     * sql文件路径
     */
    private String sqlFilePath;
    /**
     * 项目src目录的路径(默认可以从pom取，如果要生成在其他地方可自定义)
     */
    private String srcJavaPath;
    /**
     * 项目root包(Application类所在包路径)
     */
    private String basePackage;
    /**
     * 实体包名
     */
    private String entityPackageName;
    /**
     * 实体名称后缀
     */
    private String entitySuffix;
    private Map<String, String> typeMappings;
    /**
     * mybatis的配置
     */
    private MybatisPlusConfig mybatisPlusConfig = new MybatisPlusConfig();

    public GlobalConfig() {
        log.info("orm generate config init");
    }

    /**
     * @return 实体输出目录路径
     */
    public String getEntityDirPath() {
        return getBasePackagePath() +
                entityPackageName.replace(".", File.separator) +
                File.separator;
    }

    /**
     * @return 主包路径
     */
    private String getBasePackagePath() {
        return srcJavaPath
                + File.separator
                + Optional.ofNullable(basePackage).orElse("").replace(".", File.separator)
                + File.separator;
    }

}
