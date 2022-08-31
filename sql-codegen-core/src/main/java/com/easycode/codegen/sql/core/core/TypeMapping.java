package com.easycode.codegen.sql.core.core;

import java.util.HashMap;
import java.util.Map;

/**
 * @class-name: TypeMapping
 * @description:
 * @author: Mr.Zeng
 * @date: 2022/4/15 13:54
 */
public class TypeMapping {

    /**
     * java类型映射
     */
    public static final Map<String, String> INSTANCE = new HashMap<String, String>() {{
        put("varchar", "String");
        put("char", "String");

        put("tinytext", "String");
        put("mediumtext", "String");
        put("text", "String");
        put("longtext", "String");

        put("tinyblob", "byte[]");
        put("mediumblob", "byte[]");
        put("blob", "byte[]");
        put("longblob", "byte[]");

        put("int", "Integer");
        put("int@unsigned", "Integer");
        put("integer", "Integer");
        put("integer@unsigned", "Long");
        put("mediumint", "Integer");
        put("mediumint@unsigned", "Long");
        put("tinyint", "Integer");
        put("smallint", "Integer");
        put("bigint", "Long");
        put("bigint@unsigned", "Long");

        put("float", "Float");
        put("double", "Double");
        put("decimal", "java.math.BigDecimal");

        put("boolean", "Boolean");
        put("bit", "boolean");

        put("date", "java.time.LocalDate");
        put("time", "java.time.LocalTime");
        put("datetime", "java.time.LocalDateTime");
        put("timestamp", "java.util.Date");
    }};


}
