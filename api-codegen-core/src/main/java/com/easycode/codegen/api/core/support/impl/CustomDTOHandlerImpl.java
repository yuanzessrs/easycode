package com.easycode.codegen.api.core.support.impl;

import com.easycode.codegen.api.core.input.CodegenCustom;
import com.easycode.codegen.api.core.input.GlobalConfig;
import com.easycode.codegen.api.core.output.Dto;
import com.easycode.codegen.api.core.output.ResolveResult;
import com.easycode.codegen.api.core.support.IExtendHandler;
import com.easycode.codegen.api.core.util.AnnotationUtils;

import java.util.Collections;
import java.util.Optional;

/**
 * @class-name: CustomDTOHandlerImpl
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-09-15 18:37
 */
public class CustomDTOHandlerImpl implements IExtendHandler {

    @Override
    public void handle(GlobalConfig config, ResolveResult resolveResult) {
        Optional.ofNullable(config).map(GlobalConfig::getCustom).map(CodegenCustom::getDto).ifPresent(customDTO -> {
            Optional.ofNullable(resolveResult).map(ResolveResult::getDtos).orElse(Collections.emptyList()).forEach(dto -> {
                handle(dto, customDTO);
            });
        });
    }

    void handle(Dto dto, CodegenCustom.DTO customDTO) {
        if (Boolean.TRUE.equals(customDTO.getEnabledLombokGetter())) {
            dto.setHideGetFiledMethod(true);
            dto.getAnnotations().add(AnnotationUtils.lombokGetter());
        }
        if (Boolean.TRUE.equals(customDTO.getEnabledLombokSetter())) {
            dto.setHideSetFiledMethod(true);
            dto.getAnnotations().add(AnnotationUtils.lombokSetter());
        }
        Optional.ofNullable(dto.getInnerDtos()).orElse(Collections.emptyList()).forEach(innerDTO -> handle(innerDTO, customDTO));
    }

}
