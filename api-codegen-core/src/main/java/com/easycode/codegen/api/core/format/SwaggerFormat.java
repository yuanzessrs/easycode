package com.easycode.codegen.api.core.format;

import com.easycode.codegen.api.core.beans.SwaggerResource;
import com.easycode.codegen.api.core.input.GlobalConfig;
import com.easycode.codegen.api.core.input.SwaggerOption;
import com.easycode.codegen.api.core.input.SwaggerOption.YamlToJsonConfig;
import com.easycode.codegen.api.core.util.SwaggerUtils;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.springframework.util.ObjectUtils;

/**
 * @class-name: SwaggerFormat
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-16 17:39
 */
public class SwaggerFormat {

    public static void run(GlobalConfig config) {
        Optional.ofNullable(config.getSourceToJson()).ifPresent(sourceToJson -> {
            yamlToJson(SwaggerUtils.scanResources(config.getDefinitionPath()), sourceToJson);
        });
        Optional.ofNullable(config.getSwaggerOption()).map(SwaggerOption::getYamlToJson).ifPresent(sourceToJson -> {
            yamlToJson(SwaggerUtils.scanResources(config.getDefinitionPath()), sourceToJson);
        });
    }

    private static void yamlToJson(List<SwaggerResource> swaggerResources, YamlToJsonConfig sourceToJson) {
        Optional.ofNullable(sourceToJson.getEnabled()).filter(Boolean.TRUE::equals).ifPresent(o -> {
            Map<Swagger, File> swaggerFileMap = swaggerResources.stream()
                    .collect(Collectors.toMap(SwaggerResource::getModel, SwaggerResource::getFile));
            List<Swagger> swaggers = swaggerResources.stream()
                    .map(SwaggerResource::getModel)
                    .collect(Collectors.toList());
            Optional.ofNullable(sourceToJson.getPath()).ifPresent(path -> {
                Optional.ofNullable(path.getReplace()).ifPresent(replace -> {
                    if (ObjectUtils.isEmpty(replace.getSource()) || ObjectUtils.isEmpty(replace.getTarget())) {
                        throw new RuntimeException("source|target of source-to-json/path/replace must have a value");
                    }
                    swaggers.forEach(swagger -> {
                        Map<String, Path> processedPaths = new HashMap<>();
                        swagger.getPaths().forEach((name, val) -> {
                            name = name.replace(replace.getSource(), replace.getTarget());
                            processedPaths.put(name, val);
                        });
                        swagger.getPaths().clear();
                        swagger.getPaths().putAll(processedPaths);
                    });

                });
            });
            Optional.ofNullable(sourceToJson.getParam()).ifPresent(param -> {
                Optional.ofNullable(param.getFilter()).ifPresent(filter -> {
                    Set<String> nameSelector = Optional.ofNullable(filter.getNameSelector())
                            .map(HashSet::new)
                            .orElseGet(HashSet::new);
                    if (!ObjectUtils.isEmpty(nameSelector)) {
                        swaggers.forEach(swagger -> {
                            Optional.ofNullable(swagger.getParameters())
                                    .ifPresent(parameters -> nameSelector.forEach(parameters::remove));
                            swagger.getPaths().forEach((k, v) -> {
                                v.getOperationMap().forEach((opName, op) -> {
                                    op.getParameters().removeIf(parameter -> nameSelector.contains(parameter.getName()));
                                });
                            });
                        });
                    }

                });
            });

            Optional.ofNullable(sourceToJson.getResponse()).ifPresent(response -> {
                Optional.ofNullable(response.getDescriptionReplace()).ifPresent(descReplace -> {
                    if (ObjectUtils.isEmpty(descReplace.getSource()) || ObjectUtils.isEmpty(descReplace.getTarget())) {
                        throw new RuntimeException("source|target of source-to-json/response/descriptionReplace must have a value");
                    }
                    swaggers.forEach(swagger -> {
                        swagger.getPaths().forEach((_1, path) -> {
                            path.getOperationMap().forEach((_2, operation) -> {
                                operation.getResponses().forEach((_3, resp) -> {
                                    String finalDesc = Optional.ofNullable(resp.getDescription()).orElse("").replace(descReplace.getSource(), descReplace.getTarget());
                                    resp.setDescription(finalDesc);
                                });
                            });
                        });
                    });

                });
            });

            swaggerFileMap.forEach(((swagger, file) -> {
                File jsonFile = createJsonFileByYamlFile(file, sourceToJson.getOutputPath());
                try {
                    FileUtils.writeStringToFile(jsonFile, Json.pretty(swagger), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("write json-file fail. yaml-file: " + file.getAbsolutePath() + "  json-file: " + jsonFile.getAbsolutePath());
                }
            }));

        });
    }

    private static File createJsonFileByYamlFile(File yamlFile, String outputPath) {
        File parent = Optional.ofNullable(outputPath).map(File::new).orElse(yamlFile.getParentFile());
        if (parent.isDirectory() && !parent.exists()) {
            parent.mkdirs();
        }
        String name = yamlFile.getName();
        name = name.split("\\.")[0];
        return new File(parent, name + ".json");
    }

}
