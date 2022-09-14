package com.easycode.codegen.api.core.support.impl;

import com.easycode.codegen.api.core.input.CodegenCustom;
import com.easycode.codegen.api.core.input.GlobalConfig;
import com.easycode.codegen.api.core.constants.GlobalConstants;
import com.easycode.codegen.api.core.output.ResolveResult;
import com.easycode.codegen.api.core.output.Dto;
import com.easycode.codegen.api.core.support.IExtendHandler;
import com.easycode.codegen.api.core.util.AnnotationUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @class-name: CustomDtoToStringHandlerImpl
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-13 20:43
 */
public class CustomDtoToStringHandlerImpl implements IExtendHandler {

    @Override
    public void handle(GlobalConfig config, ResolveResult resolveResult) {
        Optional.ofNullable(config).map(GlobalConfig::getCustom).map(CodegenCustom::getDto).map(CodegenCustom.DTO::getToString).ifPresent(ts -> {
            List<Dto> dtos = Optional.ofNullable(resolveResult).map(ResolveResult::getDtos).orElse(Collections.emptyList());
            // custom toString
            Optional.ofNullable(ts.getCustom()).ifPresent(custom -> dtos.forEach((dto) -> {
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
                        .map((f) -> f.split(GlobalConstants.SPLIT_REGEX))
                        .map(Arrays::asList)
                        .orElse(Collections.emptyList()));
                Optional.ofNullable(lombok.getExcludeFields()).ifPresent(excludeFields::addAll);
                dtos.forEach(dto -> {
                    List<String> values = dto.getFields().stream()
                            .filter(filed -> excludeFields.contains(filed.getName())
                                    || Optional.ofNullable(filed.getAliasValues()).orElse(Collections.emptyList())
                                    .stream().anyMatch(excludeFields::contains))
                            .map(Dto.Field::getName)
                            .collect(Collectors.toList());
                    if (ObjectUtils.isEmpty(values)) {
                        dto.getAnnotations().add(AnnotationUtils.lombokToString());
                    } else {
                        dto.getAnnotations().add(AnnotationUtils.lombokToStringWithExclude(values));
                    }
                });
            });
        });
    }

}
