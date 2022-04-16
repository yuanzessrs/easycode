package com.easycode.codegen.sql.core.enumuration;

import com.easycode.codegen.sql.core.core.sql2meta.ISqlToMetaProcessor;
import com.easycode.codegen.sql.core.core.sql2meta.impl.SqlToMetaProcessorMysqlImpl;
import java.util.function.Supplier;
import lombok.Getter;

/**
 * @EnumName: DB
 * @Description: 数据库
 * @Author: Mr.Zeng
 * @Date: 2021-04-17 22:54
 */
@Getter
public enum DB {
    /**
     * mysql
     */
    MYSQL("mysql", SqlToMetaProcessorMysqlImpl::new);

    private final String id;

    private final Supplier<ISqlToMetaProcessor<?>> processorCreator;

    DB(String id, Supplier<ISqlToMetaProcessor<?>> processorCreator) {
        this.id = id;
        this.processorCreator = processorCreator;
    }
}
