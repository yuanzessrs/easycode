package com.easycode.codegen.api.core.config;

import com.easycode.codegen.api.core.config.plugin.DtoStringFieldCheckPlugin;
import lombok.Data;

/**
 * @class-name: Plugins
 * @description:
 * @author: Mr.Zeng
 * @date: 2022/3/26 18:22
 */
@Data
public class Plugins {

    private DtoStringFieldCheckPlugin dtoStringFieldChecker;

}

