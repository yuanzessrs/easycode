package com.easycode.codegen.api.core.util;

import com.easycode.codegen.api.core.constants.SwaggerConstants;
import com.easycode.codegen.utils.ClassUtils;
import com.easycode.codegen.utils.FormatUtils;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.parser.SwaggerParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.easycode.codegen.api.core.util.SwaggerVendorExtensions.getXDefault;

/**
 * @ClassName: SwaggerUtils
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-19 18:34
 */
public class SwaggerUtils {


    public static OpenAPI toOpenAPI(File file) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true); // implicit
//        parseOptions.setResolveFully(true);
        return new OpenAPIV3Parser().read(file.getAbsolutePath(), null, parseOptions);
    }

    public static Swagger toSwagger(File file) {
        io.swagger.parser.util.ParseOptions parseOptions = new io.swagger.parser.util.ParseOptions();
        parseOptions.setResolve(true); // implicit
        return new SwaggerParser().read(file.getAbsolutePath(), null, parseOptions);
    }

    /**
     * 扫描swagger文件
     *
     * @param swaggerApiDirPath swagger文件目录
     * @return swagger files
     */
    @SneakyThrows
    public static List<File> scan(String swaggerApiDirPath) {
        File apiResourceDir = new File(swaggerApiDirPath);
        FileUtils.forceMkdir(apiResourceDir);
        File[] swaggerFiles = apiResourceDir.listFiles((file, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        Objects.requireNonNull(swaggerFiles, "没有找到swagger定义文档");
        return Arrays.asList(swaggerFiles);
    }

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

    public static String getPropertyDefaultValue(Schema<?> property) {
        return Optional.ofNullable(ClassUtils.getValue(property, SwaggerConstants.DEFAULT_VALUE_FIELD))
                .map(Object::toString)
                .orElseGet(() -> getXDefault(property.getExtensions()));
    }

}
