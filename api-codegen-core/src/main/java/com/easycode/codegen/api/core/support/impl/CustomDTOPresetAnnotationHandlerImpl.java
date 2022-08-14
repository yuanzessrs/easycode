package com.easycode.codegen.api.core.support.impl;

import com.easycode.codegen.api.core.config.CustomConfig;
import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.meta.AnnotationDefinition;
import com.easycode.codegen.api.core.meta.Dto;
import com.easycode.codegen.api.core.meta.ResolveResult;
import com.easycode.codegen.api.core.support.IExtendHandler;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @class-name: CustomDTOPresetAnnotationHandlerImpl
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-14 21:54
 */
public class CustomDTOPresetAnnotationHandlerImpl implements IExtendHandler {

    @Override
    public void handle(GlobalConfig config, ResolveResult resolveResult) {
        Optional.ofNullable(config).map(GlobalConfig::getCustom).map(CustomConfig::getDto).map(CustomConfig.DTO::getPresetAnnotations).ifPresent(presetAnnotations -> {
            List<Dto> dtos = Optional.ofNullable(resolveResult).map(ResolveResult::getDtos).orElse(Collections.emptyList());
            List<AnnotationDefinition> annotationDefinitions = presetAnnotations.stream().map(annotation -> {
                AnnotationDefinition annotationDefinition = new AnnotationDefinition();
                annotationDefinition.setAnnotationName(annotation.getName());
                Optional.ofNullable(annotation.getImports()).ifPresent(annotationDefinition.getImports()::add);
                Optional.ofNullable(annotation.getProperties()).orElse(Collections.emptyList()).forEach(property -> {
                    if (ObjectUtils.isEmpty(property.getValues())) {
                        annotationDefinition.addProperty(property.getKey(), property.getValue(), property.isEnabledQuotes());
                    } else {
                        annotationDefinition.addProperty(property.getKey(), property.getValues(), property.isEnabledQuotes());
                    }
                });
                return annotationDefinition;
            }).collect(Collectors.toList());
            dtos.forEach(dto -> handle(dto, annotationDefinitions));
        });
    }

    private void handle(Dto dto, List<AnnotationDefinition> annotationDefinitions) {
        dto.getAnnotations().add(annotationDefinitions);
        dto.getInnerDtos().forEach(innerDto -> handle(innerDto, annotationDefinitions));
    }

}
