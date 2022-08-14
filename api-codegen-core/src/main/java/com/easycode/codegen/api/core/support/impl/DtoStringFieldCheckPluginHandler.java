package com.easycode.codegen.api.core.support.impl;

import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.config.Plugins;
import com.easycode.codegen.api.core.config.plugin.DtoStringFieldCheckPlugin;
import com.easycode.codegen.api.core.constants.GlobalConstants;
import com.easycode.codegen.api.core.meta.AnnotationDefinition;
import com.easycode.codegen.api.core.meta.HandlerMethod;
import com.easycode.codegen.api.core.meta.ResolveResult;
import com.easycode.codegen.api.core.support.IExtendHandler;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @class-name: DtoStringFieldCheckPluginHandler
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-13 20:28
 */
public class DtoStringFieldCheckPluginHandler implements IExtendHandler {

    @Override
    public void handle(GlobalConfig config, ResolveResult resolveResult) {
        Optional.ofNullable(config.getPlugins()).map(Plugins::getDtoStringFieldChecker).ifPresent(plugin -> {
            Set<String> returnTypes = resolveResult.getClasses().stream()
                    .flatMap((hc) -> hc.getHandlerMethods().stream())
                    .map(HandlerMethod::getHandlerMethodReturn)
                    .map(HandlerMethod.Return::getType)
                    .collect(Collectors.toSet());

            Set<String> targetFields = new HashSet<>();
            targetFields.addAll(Optional.ofNullable(plugin.getField())
                    .map(String::trim)
                    .map((f) -> f.replaceAll("\r", ""))
                    .map((f) -> f.replaceAll("\n", ""))
                    .map((f) -> f.split(GlobalConstants.SPLIT_REGEX))
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList()));
            targetFields.addAll(Optional.ofNullable(plugin.getFields()).orElse(Collections.emptyList()));

            if (!CollectionUtils.isEmpty(targetFields)) {
                Optional.ofNullable(resolveResult.getDtos()).ifPresent((dtos) -> {
                    dtos.stream().filter((dto) -> returnTypes.contains(dto.getName())).forEach((dto) -> {
                        dto.getFields().forEach((field) -> {
                            List<String> alias = Optional.ofNullable(field.getAliasValues()).orElse(Collections.emptyList());
                            if (alias.stream().anyMatch(targetFields::contains)) {
                                boolean isError = !"String".equals(field.getType()) && !"List<String>".equals(field.getType());
                                if (isError) {
                                    Set<String> filterByAnnotations = Optional
                                            .ofNullable(plugin.getFilterByAnnotations())
                                            .orElse(Collections.emptyList())
                                            .stream()
                                            .map(DtoStringFieldCheckPlugin.FilterAnnotation::toString)
                                            .collect(Collectors.toSet());
                                    isError = field.getAnnotations().get()
                                            .stream()
                                            .map(AnnotationDefinition::toString)
                                            .noneMatch(filterByAnnotations::contains);
                                }
                                if (isError) {
                                    String msg = String.format("dto: %s field: %s must be of type String", dto.getName(), field.getName());
                                    throw new RuntimeException(msg);
                                }
                            }

                        });
                    });
                });
            }
        });
    }

}
