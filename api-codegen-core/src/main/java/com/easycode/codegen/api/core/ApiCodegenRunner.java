package com.easycode.codegen.api.core;


import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.config.PathWrapper;
import com.easycode.codegen.api.core.enums.GenerateType;
import com.easycode.codegen.api.core.meta.ResolveResult;
import com.easycode.codegen.api.core.resolver.ResolverContext;
import com.easycode.codegen.api.core.resolver.impl.SwaggerResolver;
import com.easycode.codegen.api.core.support.ExtendManager;
import com.easycode.codegen.utils.EnumUtils;
import com.easycode.codegen.utils.VelocityUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
        switch (EnumUtils.getEnum(GenerateType.class, config.getGenerateType().toLowerCase())) {
            case SPRING_MVC:
                this.generateControllerFile(config, resolveResult);
                this.generateServiceFile(config, resolveResult);
                this.generateDtoFile(config, resolveResult);
                break;
            case FEIGN_CLIENT:
                this.generateFeignClientFile(config, resolveResult);
                this.generateDtoFile(config, resolveResult);
        }

    }

    /**
     * generate  file
     *
     * @param config        全局配置
     * @param resolveResult 解析结果
     */
    private void generateControllerFile(GlobalConfig config, ResolveResult resolveResult) {
        PathWrapper pathWrapper = new PathWrapper(config);
        resolveResult.getClasses().forEach((controllerMeta) -> {
            Map<String, Object> params = new HashMap<>(8);
            params.put("handlerClass", controllerMeta);
            params.put("config", config);
            File file = new File(pathWrapper.getControllerPackagePath() + controllerMeta.getName() + ".java");
            VelocityUtils.render("template/Controller.vm", params, file);
        });
    }

    /**
     * generate  file
     *
     * @param config        全局配置
     * @param resolveResult 解析结果
     */
    private void generateServiceFile(GlobalConfig config, ResolveResult resolveResult) {
        PathWrapper pathWrapper = new PathWrapper(config);
        resolveResult.getClasses().forEach((controllerMeta) -> {
            Map<String, Object> params = new HashMap<>(8);
            params.put("handlerClass", controllerMeta);
            params.put("config", config);
            File file = new File(pathWrapper.getServicePackagePath() + controllerMeta.getServiceName() + ".java");
            VelocityUtils.render("template/IService.vm", params, file);
        });
    }

    /**
     * generate  file
     *
     * @param config        全局配置
     * @param resolveResult 解析结果
     */
    private void generateDtoFile(GlobalConfig config, ResolveResult resolveResult) {
        PathWrapper pathWrapper = new PathWrapper(config);
        resolveResult.getDtos().forEach((dto) -> {
            log.info("dto name: " + dto.getName());
            Map<String, Object> params = new HashMap<>(8);
            params.put("definition", dto);
            params.put("config", config);
            File file = new File(pathWrapper.getDtoPackagePath() + dto.getName() + ".java");
            VelocityUtils.render("template/Dto.vm", params, file);
        });
    }

    /**
     * generate  file
     *
     * @param config        全局配置
     * @param resolveResult 解析结果
     */
    private void generateFeignClientFile(GlobalConfig config, ResolveResult resolveResult) {
        PathWrapper pathWrapper = new PathWrapper(config);
        resolveResult.getClasses().forEach((controllerMeta) -> {
            Map<String, Object> params = new HashMap<>(8);
            params.put("handlerClass", controllerMeta);
            params.put("config", config);
            File file = new File(pathWrapper.getFeignClientPackagePath() + controllerMeta.getFeignClientName() + ".java");
            VelocityUtils.render("template/FeignClient.vm", params, file);
        });
    }
}
