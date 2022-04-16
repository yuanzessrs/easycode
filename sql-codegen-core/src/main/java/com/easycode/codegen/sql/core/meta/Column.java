package com.easycode.codegen.sql.core.meta;

import com.easycode.codegen.utils.FormatUtils;
import lombok.Data;

/**
 * @ClassName: Column
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-17 20:49
 */
@Data
public class Column {

    private Boolean isPrimaryKey;

    private Boolean isAutoIncrement;

    private String fieldName;

    private String columnName;

    private String comment;

    private String dbType;

    private String dbTypeDesc;

    private String javaType;

    private Boolean isAutoFillWhenInsert;

    private Boolean isAutoFillWhenUpdate;

    private Boolean isAutoFillWhenInsertOrUpdate;

    private Boolean isLogicalDeleteField;

    private String logicalDeletedValue;

    private String logicalNotDeletedValue;

    /**
     * @return java type name
     */
    public String getTypeName() {
        return javaType.contains(".") ? javaType.substring(javaType.lastIndexOf(".") + 1) : javaType;
    }

    /**
     * @return import
     */
    public String getImport() {
        return javaType.contains(".") ? javaType : null;
    }

    public String upperCamelCaseName() {
        return FormatUtils.lowerCamelToUpperCamel(fieldName);
    }

}
