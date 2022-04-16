package com.easycode.codegen.api.core.util;

import com.easycode.codegen.api.core.constants.SwaggerConstants;
import com.easycode.codegen.utils.ClassUtils;
import com.easycode.codegen.utils.FormatUtils;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.parser.SwaggerParser;
import java.util.Optional;

/**
 * @ClassName: SwaggerUtils
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-19 18:34
 */
public class SwaggerUtils {

    public static Swagger parseSwagger(String content) {
        return new SwaggerParser().parse(content);
    }

    public static String getClassNameFromHandlerMethodName(String handlerMethodName) {
        return FormatUtils.lowerCamelToUpperCamel(handlerMethodName) + SwaggerConstants.QUEUE_PARAM_DTO_SUFFIX;
    }

    public static String getClassNameFromRefPath(String refPath) {
        return getClassNameFromDefinitionName(refPath.replace("#/definitions/", ""));
    }

    public static String getClassNameFromDefinitionName(String definitionName) {
        return definitionName;
    }

    public static String wrapControllerClassName(String name) {
        return name + "AutogenController";
    }

    public static String wrapControllerServiceClassName(String name) {
        return "I" + name + "AutogenService";
    }

    public static String wrapFeignClientClassName(String name) {
        return "I" + name + "AutogenFeignClient";
    }

    /**
     * 获取swagger属性默认值
     *
     * @param property swagger属性
     * @return 默认值
     */
    public static String getPropertyDefaultValue(Property property) {
        return Optional.ofNullable(ClassUtils.getValue(property, SwaggerConstants.DEFAULT_VALUE_FIELD))
                .map(Object::toString)
                .orElse(null);
    }

}
