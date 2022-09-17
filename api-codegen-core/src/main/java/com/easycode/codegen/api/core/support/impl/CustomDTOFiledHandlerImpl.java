package com.easycode.codegen.api.core.support.impl;

import com.easycode.codegen.api.core.input.CodegenCustom;
import com.easycode.codegen.api.core.input.GlobalConfig;
import com.easycode.codegen.api.core.output.Dto;
import com.easycode.codegen.api.core.output.ResolveResult;
import com.easycode.codegen.api.core.support.IExtendHandler;
import com.easycode.codegen.api.core.util.AnnotationUtils;
import com.easycode.codegen.utils.FormatUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.Optional;

/**
 * @class-name: CustomDTOFiledHandlerImpl
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-09-15 18:37
 */
public class CustomDTOFiledHandlerImpl implements IExtendHandler {

    @Override
    public void handle(GlobalConfig config, ResolveResult resolveResult) {
        Optional.ofNullable(config).map(GlobalConfig::getCustom).map(CodegenCustom::getDto).map(CodegenCustom.DTO::getField).ifPresent(customField -> {
            Optional.ofNullable(resolveResult).map(ResolveResult::getDtos).orElse(Collections.emptyList()).forEach(dto -> {
                handle(dto, customField);
            });
        });
    }

    void handle(Dto dto, CodegenCustom.DTO.Field customField) {
        if (Boolean.TRUE.equals(customField.getAutoRenameToLowerCamel())) {
            Optional.ofNullable(dto.getFields()).orElse(Collections.emptyList()).forEach(field -> {
                if (ObjectUtils.isEmpty(field.getAliasValues())) {
                    String originalName = field.getName();
                    String finalName = FormatUtils.toLowerCamel(originalName);
                    if (!finalName.equals(originalName)) {
                        field.setName(finalName);
                        field.setAliasValues(Collections.singletonList(originalName));
                        if (dto.getIsGetParamsDTO()) {
                            field.getAnnotations().add(AnnotationUtils.getParamAlias(originalName));
                        } else {
                            field.getAnnotations().add(AnnotationUtils.jsonProperty(originalName));
                        }
                    }
                }
            });
        }
        Optional.ofNullable(dto.getInnerDtos()).orElse(Collections.emptyList()).forEach(innerDTO -> handle(innerDTO, customField));
    }

}
