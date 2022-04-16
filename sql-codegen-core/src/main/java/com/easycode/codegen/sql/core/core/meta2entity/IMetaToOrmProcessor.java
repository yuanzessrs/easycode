package com.easycode.codegen.sql.core.core.meta2entity;

import com.easycode.codegen.sql.core.config.GlobalConfig;
import com.easycode.codegen.sql.core.meta.Table;
import java.util.List;

/**
 * @InterfaceName: IMetaToOrmProcessor
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-18 23:50
 */
public interface IMetaToOrmProcessor<T> {

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
     * 根据元数据生成实体文件
     *
     * @param tables 元数据
     */
    void generate(List<Table> tables);

}
