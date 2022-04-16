package com.easycode.codegen.api.core.util;

import java.util.Optional;

/**
 * @ClassName: TypeResolver
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-23 17:07
 */
public class TypeResolver {

    public static String getType(String importPath) {
        String[] split = Optional.ofNullable(importPath).orElse("").split("\\.");
        return split.length > 0 ? split[split.length - 1] : null;
    }

}
