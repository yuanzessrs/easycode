package com.easycode.codegen.sql.core.core.sql2meta;

import com.easycode.codegen.sql.core.meta.Table;
import com.easycode.codegen.sql.core.config.GlobalConfig;
import java.util.List;

/**
 * @InterfaceName: ISqlToMetaProcessor
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-18 23:48
 */
public interface ISqlToMetaProcessor<T> {

    /**
     * 设置全局config
     *
     * @param config 全局配置
     */
    void setConfig(GlobalConfig config);

    /**
     * 设置上下文
     *
     * @param t 上下文
     */
    void setContext(T t);

    /**
     * 根据建表sql生成元数据
     *
     * @param createTableSql 建表sql
     * @return 表元数据
     */
    List<Table> convertTable(String createTableSql);

    /**
     * 类型转换
     *
     * @param dbType     数据库类型
     * @param isUnsigned 是否无符号
     * @return javaType
     */
    String dbTypeToJavaType(String dbType, boolean isUnsigned);
}
