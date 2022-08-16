package com.easycode.codegen.api.core;

import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.format.SwaggerFormat;
import com.easycode.codegen.api.core.meta.ResolveResult;
import com.easycode.codegen.api.core.output.SwaggerOutput;
import com.easycode.codegen.api.core.resolver.ResolverContext;
import com.easycode.codegen.api.core.resolver.impl.SwaggerResolver;
import com.easycode.codegen.api.core.support.ExtendManager;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: ApiCodegenRunner
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-06 22:41
 */
@Slf4j
public class ApiCodegenRunner {

    /**
     * 开始执行api代码生成
     *
     * @param config 全局配置
     */
    public void start(GlobalConfig config) {
        ResolverContext context = ResolverContext.builder().definitionPath(config.getDefinitionPath()).build();
        ResolveResult resolveResult = new SwaggerResolver(context).resolve();
        ExtendManager.handle(config, resolveResult);
        SwaggerOutput.run(config, resolveResult);
        SwaggerFormat.run(config);
    }

}
