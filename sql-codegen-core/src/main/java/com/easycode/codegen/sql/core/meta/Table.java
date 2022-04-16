package com.easycode.codegen.sql.core.meta;

import java.util.List;
import lombok.Data;

/**
 * @ClassName: Table
 * @Description: 表对应的属性，生成Entity使用
 * @Author: Mr.Zeng
 * @Date: 2021-04-17 20:48
 */
@Data
public class Table {

    private String className;

    private String tableName;

    private String tableComment;

    private List<Column> columns;

    private List<FiledMapping> fieldMappings;

    private List<String> imports;

    @Data
    public static class FiledMapping {

        private String key;
        private String value;

    }

}
