package com.easycode.codegen.api.core.output;

import com.easycode.codegen.api.core.input.GlobalConfig;
import com.easycode.codegen.api.core.input.PathWrapper;
import com.easycode.codegen.api.core.enums.GenerateType;
import com.easycode.codegen.utils.EnumUtils;
import com.easycode.codegen.utils.VelocityUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @class-name: SwaggerOutput
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-16 18:59
 */
@Slf4j
public class SwaggerOutput {

    public static void run(GlobalConfig config, ResolveResult resolveResult) {
        switch (EnumUtils.getEnum(GenerateType.class, config.getGenerateType().toLowerCase())) {
            case SPRING_MVC:
                generateControllerFile(config, resolveResult);
                generateServiceFile(config, resolveResult);
                generateDtoFile(config, resolveResult);
                break;
            case FEIGN_CLIENT:
                generateFeignClientFile(config, resolveResult);
                generateDtoFile(config, resolveResult);
        }
    }

    /**
     * generate  file
     *
     * @param config        全局配置
     * @param resolveResult 解析结果
     */
    private static void generateControllerFile(GlobalConfig config, ResolveResult resolveResult) {
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
    private static void generateServiceFile(GlobalConfig config, ResolveResult resolveResult) {
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
    private static void generateDtoFile(GlobalConfig config, ResolveResult resolveResult) {
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
    private static void generateFeignClientFile(GlobalConfig config, ResolveResult resolveResult) {
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
