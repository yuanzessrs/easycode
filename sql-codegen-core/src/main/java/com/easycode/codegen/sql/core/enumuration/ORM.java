package com.easycode.codegen.sql.core.enumuration;

import com.easycode.codegen.sql.core.core.meta2entity.IMetaToOrmProcessor;
import com.easycode.codegen.sql.core.core.meta2entity.impl.MetaToOrmProcessorMybatisPlusImpl;
import java.util.function.Supplier;
import lombok.Getter;

/**
 * @EnumName: ORM
 * @Description: orm框架
 * @Author: Mr.Zeng
 * @Date: 2021-04-19 10:07
 */
@Getter
public enum ORM {

    /**
     * mybatis plus
     */
    MYBATIS_PLUS("mybatis-plus", MetaToOrmProcessorMybatisPlusImpl::new);

    private final String id;

    private final Supplier<IMetaToOrmProcessor<?>> processorCreator;

    ORM(String id, Supplier<IMetaToOrmProcessor<?>> processorCreator) {
        this.id = id;
        this.processorCreator = processorCreator;
    }
}
