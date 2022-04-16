package com.easycode.codegen.api.core;


import com.easycode.codegen.api.core.config.CustomConfig;
import com.easycode.codegen.api.core.config.CustomConfig.ToString;
import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.config.PathWrapper;
import com.easycode.codegen.api.core.config.PluginConfig;
import com.easycode.codegen.api.core.config.plugin.DtoStringFieldCheckPlugin.FilterAnnotation;
import com.easycode.codegen.api.core.enums.GenerateType;
import com.easycode.codegen.api.core.meta.ApiResolveResult;
import com.easycode.codegen.api.core.meta.Dto.Field;
import com.easycode.codegen.api.core.meta.HandlerMethod;
import com.easycode.codegen.api.core.meta.HandlerMethod.Return;
import com.easycode.codegen.api.core.resolver.ResolverContext;
import com.easycode.codegen.api.core.resolver.impl.SwaggerResolver;
import com.easycode.codegen.api.core.util.AnnotationUtils;
import com.easycode.codegen.utils.EnumUtils;
import com.easycode.codegen.utils.VelocityUtils;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

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
        ResolverContext context = new ResolverContext();
        context.setDefinitionFilesDirPath(config.getApiDefineDirPath());
        context.setApplicationName(config.getApplicationName());
        ApiResolveResult apiResolveResult = (new SwaggerResolver(context)).resolve();
        this.customDto(apiResolveResult, config);
        this.checkDtoStringField(apiResolveResult, config);
        switch (EnumUtils.getEnum(GenerateType.class, config.getGenerateType().toLowerCase())) {
            case SPRING_MVC:
                this.generateControllerFile(config, apiResolveResult);
                this.generateServiceFile(config, apiResolveResult);
                this.generateDtoFile(config, apiResolveResult);
                break;
            case FEIGN_CLIENT:
                this.generateFeignClientFile(config, apiResolveResult);
                this.generateDtoFile(config, apiResolveResult);
        }

    }

    private void checkDtoStringField(ApiResolveResult apiResolveResult, GlobalConfig config) {
        Set<String> returnTypes = apiResolveResult.getClasses().stream()
                .flatMap((hc) -> hc.getHandlerMethods().stream())
                .map(HandlerMethod::getHandlerMethodReturn)
                .map(Return::getType)
                .collect(Collectors.toSet());
        Optional.ofNullable(config).map(GlobalConfig::getPlugins).map(PluginConfig::getDtoStringFieldChecker)
                .ifPresent((dtoStringFieldCheckPlugin) -> {
                    Set<String> targetFields = new HashSet();
                    targetFields.addAll(Optional.ofNullable(dtoStringFieldCheckPlugin.getField())
                            .map(String::trim)
                            .map((f) -> f.replaceAll("\r", ""))
                            .map((f) -> f.replaceAll("\n", ""))
                            .map((f) -> f.split(";|,|；|，|、"))
                            .map(Arrays::asList)
                            .orElse(Collections.emptyList()));
                    targetFields.addAll(Optional.ofNullable(dtoStringFieldCheckPlugin.getFields())
                            .orElse(Collections.emptyList()));
                    if (!CollectionUtils.isEmpty(targetFields)) {
                        Optional.ofNullable(apiResolveResult.getDtos()).ifPresent((dtos) -> {
                            dtos.stream().filter((dto) -> returnTypes.contains(dto.getName())).forEach((dto) -> {
                                dto.getFields().forEach((field) -> {
                                    if (targetFields.contains(field.getName())) {
                                        boolean isError = !"String".equals(field.getType()) && !"List<String>"
                                                .equals(field.getType());
                                        if (isError) {
                                            Set<String> filterByAnnotations = Optional
                                                    .ofNullable(dtoStringFieldCheckPlugin.getFilterByAnnotations())
                                                    .orElse(Collections.emptyList())
                                                    .stream()
                                                    .map(FilterAnnotation::toString)
                                                    .collect(Collectors.toSet());
                                            isError = field.getAnnotations().get().stream()
                                                    .noneMatch((annotation) -> filterByAnnotations
                                                            .contains(annotation.toString()));
                                        }
                                        if (isError) {
                                            String msg = String
                                                    .format("dto: %s field: %s must be of type String", dto.getName(),
                                                            field.getName());
                                            throw new RuntimeException(msg);
                                        }
                                    }

                                });
                            });
                        });
                    }
                });
    }

    private void customDto(ApiResolveResult apiResolveResult, GlobalConfig config) {
        Optional.ofNullable(config)
                .map(GlobalConfig::getCustom)
                .map(CustomConfig::getDto)
                .ifPresent(dto -> {
                    customDtoToString(apiResolveResult, config);
                });
    }

    private void customDtoToString(ApiResolveResult apiResolveResult, GlobalConfig config) {
        ToString ts = config.getCustom().getDto().getToString();

        // custom toString
        Optional.ofNullable(ts.getCustom()).ifPresent(custom -> apiResolveResult.getDtos().forEach((dto) -> {
            Optional.ofNullable(custom.getExtImport()).ifPresent(extImport -> dto.getImports().add(extImport));
            Optional.ofNullable(custom.getTemplate()).ifPresent(dto::setOverrideToString);
        }));
        // lombok toString
        Optional.ofNullable(ts.getLombok()).ifPresent(lombok -> {
            Set<String> excludeFields = new HashSet<>();
            excludeFields.addAll(Optional.ofNullable(lombok.getExcludeField())
                    .map(String::trim)
                    .map((f) -> f.replaceAll("\r", ""))
                    .map((f) -> f.replaceAll("\n", ""))
                    .map((f) -> f.split(";|,|；|，|、"))
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList()));
            Optional.ofNullable(lombok.getExcludeFields()).ifPresent(excludeFields::addAll);
            apiResolveResult.getDtos().forEach(dto -> {
                List<String> values = dto.getFields().stream()
                        .filter(filed -> excludeFields.contains(filed.getName())
                                || Optional.ofNullable(filed.getAliasValues()).orElse(Collections.emptyList())
                                .stream().anyMatch(excludeFields::contains))
                        .map(Field::getName)
                        .collect(Collectors.toList());
                if (ObjectUtils.isEmpty(values)) {
                    dto.getAnnotations().add(AnnotationUtils.lombokToString());
                } else {
                    dto.getAnnotations().add(AnnotationUtils.lombokToStringWithExclude(values));
                }
            });
        });
    }

    /**
     * generate  file
     *
     * @param config           全局配置
     * @param apiResolveResult 解析结果
     */
    private void generateControllerFile(GlobalConfig config, ApiResolveResult apiResolveResult) {
        PathWrapper pathWrapper = new PathWrapper(config);
        apiResolveResult.getClasses().forEach((controllerMeta) -> {
            Map<String, Object> params = new HashMap(8);
            params.put("handlerClass", controllerMeta);
            params.put("config", config);
            File file = new File(pathWrapper.getControllerPackagePath() + controllerMeta.getName() + ".java");
            VelocityUtils.render("template/Controller.vm", params, file);
        });
    }

    /**
     * generate  file
     *
     * @param config           全局配置
     * @param apiResolveResult 解析结果
     */
    private void generateServiceFile(GlobalConfig config, ApiResolveResult apiResolveResult) {
        PathWrapper pathWrapper = new PathWrapper(config);
        apiResolveResult.getClasses().forEach((controllerMeta) -> {
            Map<String, Object> params = new HashMap(8);
            params.put("handlerClass", controllerMeta);
            params.put("config", config);
            File file = new File(pathWrapper.getServicePackagePath() + controllerMeta.getServiceName() + ".java");
            VelocityUtils.render("template/IService.vm", params, file);
        });
    }

    /**
     * generate  file
     *
     * @param config           全局配置
     * @param apiResolveResult 解析结果
     */
    private void generateDtoFile(GlobalConfig config, ApiResolveResult apiResolveResult) {
        PathWrapper pathWrapper = new PathWrapper(config);
        apiResolveResult.getDtos().forEach((dto) -> {
            log.info("dto name: " + dto.getName());
            Map<String, Object> params = new HashMap(8);
            params.put("definition", dto);
            params.put("config", config);
            File file = new File(pathWrapper.getDtoPackagePath() + dto.getName() + ".java");
            VelocityUtils.render("template/Dto.vm", params, file);
        });
    }

    /**
     * generate  file
     *
     * @param config           全局配置
     * @param apiResolveResult 解析结果
     */
    private void generateFeignClientFile(GlobalConfig config, ApiResolveResult apiResolveResult) {
        PathWrapper pathWrapper = new PathWrapper(config);
        apiResolveResult.getClasses().forEach((controllerMeta) -> {
            Map<String, Object> params = new HashMap(8);
            params.put("handlerClass", controllerMeta);
            params.put("config", config);
            File file = new File(
                    pathWrapper.getFeignClientPackagePath() + controllerMeta.getFeignClientName() + ".java");
            VelocityUtils.render("template/FeignClient.vm", params, file);
        });
    }
}
