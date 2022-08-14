package com.easycode.codegen.api.core.support.impl;

import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.meta.Dto;
import com.easycode.codegen.api.core.meta.ResolveResult;
import com.easycode.codegen.api.core.support.IExtendHandler;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @class-name: DuplicatedNameDTOCheckHandlerImpl
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-13 21:22
 */
public class DuplicatedNameDTOCheckHandlerImpl implements IExtendHandler {

    @Override
    public void handle(GlobalConfig config, ResolveResult resolveResult) {
        // 检查重名dto
        resolveResult.getDtos()
                .stream()
                .map(Dto::getName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(sameName -> {
                    throw new RuntimeException(String.format("存在同名DTO:%s，会导致文件覆盖,请检查!", sameName));
                });
    }
}
