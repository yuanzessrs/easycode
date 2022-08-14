package com.easycode.codegen.api.core.support.impl;

import com.easycode.codegen.api.core.config.CustomConfig;
import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.meta.ResolveResult;
import com.easycode.codegen.api.core.meta.Dto;
import com.easycode.codegen.api.core.support.IExtendHandler;
import com.easycode.codegen.api.core.util.AnnotationUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @class-name: CustomDTOBuilderHandlerImpl
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-13 20:40
 */
public class CustomDTOBuilderHandlerImpl implements IExtendHandler {

    @Override
    public void handle(GlobalConfig config, ResolveResult resolveResult) {
        Optional.ofNullable(config).map(GlobalConfig::getCustom).map(CustomConfig::getDto).map(CustomConfig.DTO::getBuilder).ifPresent(builder -> {
            List<Dto> dtos = Optional.ofNullable(resolveResult).map(ResolveResult::getDtos).orElse(Collections.emptyList());
            Optional.ofNullable(builder.getLombok()).ifPresent(lombok -> dtos.forEach(dto -> {
                dto.getAnnotations().add(AnnotationUtils.lombokBuilder());
                dto.getAnnotations().add(AnnotationUtils.lombokGetter());
                dto.setHasBuilder(true);
            }));
        });

    }

}
