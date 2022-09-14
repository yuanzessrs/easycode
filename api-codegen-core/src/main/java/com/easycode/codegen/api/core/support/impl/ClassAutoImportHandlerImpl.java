package com.easycode.codegen.api.core.support.impl;

import com.easycode.codegen.api.core.input.CodegenCustom;
import com.easycode.codegen.api.core.input.GlobalConfig;
import com.easycode.codegen.api.core.output.AnnotationDefinition;
import com.easycode.codegen.api.core.output.Dto;
import com.easycode.codegen.api.core.output.ResolveResult;
import com.easycode.codegen.api.core.support.IExtendHandler;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @class-name: ClassAutoImportHandlerImpl
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-13 21:02
 */
public class ClassAutoImportHandlerImpl implements IExtendHandler {

    @Override
    public void handle(GlobalConfig config, ResolveResult resolveResult) {
        Optional.ofNullable(config.getCustom()).map(CodegenCustom::getAutoImport).ifPresent(autoImport -> {
            Map<String, String> mappings = Optional.ofNullable(autoImport.getMappings()).orElse(Collections.emptyMap());

            Consumer<AnnotationDefinition> annotationAutoImportProcessor = annotationDefinition -> {
                String annotationName = annotationDefinition.getAnnotationName();
                if (mappings.containsKey(annotationName)) {
                    annotationDefinition.getImports().add(mappings.get(annotationName));
                }
            };

            // process HandlerClass common

            // process Controller class
            resolveResult.getClasses().forEach(handlerClass -> {
                handlerClass.getControllerAnnotations().get().forEach(annotationAutoImportProcessor);
                handlerClass.getHandlerMethods().forEach(handlerMethod -> {
                    handlerMethod.getControllerAnnotations().get().forEach(annotationAutoImportProcessor);
                });
            });

            // process FeignClient class

            // process Service class

            // process DTO class
            Consumer<Dto> dtoAnnotationAutoImportProcessor = dto -> {
                dto.getAnnotations().get().forEach(annotationAutoImportProcessor);
                dto.getFields().forEach(field -> {
                    field.getAnnotations().get().forEach(annotationAutoImportProcessor);
                });
            };
            Consumer<Dto> recurDtoAnnotationAutoImportProcessor = dto -> {
                dtoAnnotationAutoImportProcessor.accept(dto);
                dto.getInnerDtos().forEach(dtoAnnotationAutoImportProcessor);
            };
            resolveResult.getDtos().forEach(recurDtoAnnotationAutoImportProcessor);

        });
    }

}
