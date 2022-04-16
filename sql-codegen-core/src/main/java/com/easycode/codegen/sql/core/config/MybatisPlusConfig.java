package com.easycode.codegen.sql.core.config;

import java.util.List;
import java.util.Set;
import lombok.Data;

/**
 * @ClassName: MybatisInputConfig
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-17 21:38
 */
@Data
public class MybatisPlusConfig {

    /**
     * 逻辑删除字段field，不指定默认 del
     */
    private List<LogicDeleteColumn> logicDelCols;

    private Set<String> autoInsertFields;

    private Set<String> autoUpdateFields;

    private Set<String> autoInsertOrUpdateFields;

    private Boolean enableOutputMapper;

    private String mapperInterfacePackage;

    private String mapperXmlDirName;

    @Data
    public static class LogicDeleteColumn {

        private String columnName;

        private String deletedValue;

        private String notDeletedValue;

    }

}
