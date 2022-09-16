package com.easycode.codegen.api.core.resolver;

import com.easycode.codegen.api.core.input.Option;
import com.easycode.codegen.api.core.input.SwaggerOption;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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

    private String definitionUrl;

    private List<String> definitionUrls;

    private Option options;

    private SwaggerOption swaggerOption;

}
