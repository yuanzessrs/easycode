package com.easycode.codegen.api.core.format;

import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.util.SwaggerUtils;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import org.apache.commons.io.FileUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @class-name: SwaggerFormat
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-16 17:39
 */
public class SwaggerFormat {

    public static void run(GlobalConfig config) {
        Optional.ofNullable(config.getSourceToJson()).ifPresent(sourceToJson -> {
            Optional.ofNullable(sourceToJson.getEnabled()).filter(Boolean.TRUE::equals).ifPresent(o -> {
                Map<Swagger, File> swaggerFileMap = new HashMap<>();
                List<Swagger> swaggers = SwaggerUtils.scan(config.getDefinitionPath())
                        .stream()
                        .map(file -> {
                            Swagger swagger = SwaggerUtils.toSwagger(file);
                            swaggerFileMap.put(swagger, file);
                            return swagger;
                        }).collect(Collectors.toList());


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

                swaggerFileMap.forEach(((swagger, file) -> {
                    File jsonFile = createJsonFileByYamlFile(file, sourceToJson.getOutputPath());
                    try {
                        FileUtils.writeStringToFile(jsonFile, Json.pretty(swagger), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("write json-file fail. yaml-file: " + file.getAbsolutePath() + "  json-file: " + jsonFile.getAbsolutePath());
                    }
                }));

            });
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
