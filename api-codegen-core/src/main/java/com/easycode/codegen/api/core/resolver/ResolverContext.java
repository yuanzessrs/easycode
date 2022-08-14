package com.easycode.codegen.api.core.resolver;

import lombok.Builder;
import lombok.Getter;

/**
 * @class-name: SwaggerResolverContext
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-01-11 18:44
 */
@Builder
@Getter
public class ResolverContext {

    private String definitionPath;

}
