package com.easycode.codegen.api.core.util;

import com.easycode.codegen.api.core.constants.SwaggerConstants;
import com.easycode.codegen.api.core.meta.AnnotationDefinition;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SwaggerVendorExtensions {

    private static final Map<String, AnnotationDefinition> SUPPORT_ANNOTATION_MAP = new HashMap<String, AnnotationDefinition>() {{
        put("jsonInclude", AnnotationUtils.jsonInclude());
        put("jsonIgnore", AnnotationUtils.jsonIgnore());
    }};

    public static String getXFieldVal(Map<String, Object> vendorExtensions, String key) {
        return Optional.ofNullable(vendorExtensions)
                .map(map -> map.get("x-" + key))
                .map(Object::toString)
                .orElse(null);
    }

    public static Map<String, String> getRenameMap(Map<String, Object> vendorExtensions) {
        Object renameVal = vendorExtensions.get("x-rename-map");
        if (renameVal == null) {
            return Collections.emptyMap();
        }
        if (renameVal instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) renameVal;

            Map<String, String> renameMap = new HashMap<>();
            data.forEach((key, val) -> {
                renameMap.put(key.trim(), val.toString().trim());
            });
            return renameMap;
        }
        throw new RuntimeException("注解扩展属性（x-rename-map）的值一定是 {} 或 有值的map!");
    }

    public static String getXFormat(Map<String, Object> vendorExtensions) {
        return getXFieldVal(vendorExtensions, "format");
    }

    public static boolean hasXFormat(Map<String, Object> vendorExtensions) {
        return !ObjectUtils.isEmpty(getXFormat(vendorExtensions));
    }

    public static String getXDefault(Map<String, Object> vendorExtensions) {
        return getXFieldVal(vendorExtensions, "default");
    }

    public static String getRenameVal(Map<String, Object> vendorExtensions) {
        return getXFieldVal(vendorExtensions, "rename");
    }

    public static List<String> getImports(Map<String, Object> vendorExtensions) {
        Object it = vendorExtensions.get("x-import");
        if (ObjectUtils.isEmpty(it)) {
            return Collections.emptyList();
        }
        if (it instanceof Collection) {
            return ((Collection<?>) it).stream().map(o -> (String) o).map(String::trim).collect(Collectors.toList());
        }
        return Arrays.stream(((String) it).split(SwaggerConstants.SPLIT_REGEX))
                .filter(o -> null != o && !o.isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public static List<AnnotationDefinition> parseControllerAnnotations(Map<String, Object> vendorExtensions) {
        return parseAnnotations("x-Controller@", vendorExtensions);
    }

    public static List<AnnotationDefinition> parseFeignClientAnnotations(Map<String, Object> vendorExtensions) {
        return parseAnnotations("x-FeignClient@", vendorExtensions);
    }

    public static List<AnnotationDefinition> parseServiceAnnotations(Map<String, Object> vendorExtensions) {
        return parseAnnotations("x-Service@", vendorExtensions);
    }

    public static List<AnnotationDefinition> parseAnnotations(Map<String, Object> vendorExtensions) {
        return parseAnnotations("x-@", vendorExtensions);
    }

    public static List<AnnotationDefinition> parsePresetAnnotations(Map<String, Object> vendorExtensions) {
        String xFiledName = "x-@";
        Object val = vendorExtensions.get(xFiledName);
        if (val == null) {
            return Collections.emptyList();
        }
        Stream<String> supportAnnotationNames;
        if (val instanceof Collection) {
            supportAnnotationNames = ((Collection) val).stream();
        } else if (val instanceof String) {
            supportAnnotationNames = Stream.of(((String) val).split(SwaggerConstants.SPLIT_REGEX));
        } else {
            throw new RuntimeException(xFiledName + ", value 格式异常，仅支持 List ｜ Array ｜ String");
        }
        return supportAnnotationNames.distinct().map(supportAnnotationName -> {
            AnnotationDefinition supportAnnotation = SUPPORT_ANNOTATION_MAP.get(supportAnnotationName);
            if (supportAnnotation == null) {
                throw new RuntimeException(xFiledName + "不支持该枚举:" + supportAnnotationName);
            }
            return supportAnnotation;
        }).collect(Collectors.toList());
    }

    public static List<AnnotationDefinition> parseAnnotations(String keyPrefix, Map<String, Object> vendorExtensions) {
        return vendorExtensions.keySet()
                .stream()
                .filter(key -> key.startsWith(keyPrefix) && key.length() > keyPrefix.length())
                .map(key -> {
                    AnnotationDefinition annotation = new AnnotationDefinition();
                    annotation.setAnnotationName(key.replaceFirst(keyPrefix, ""));
                    Object val = vendorExtensions.get(key);
                    if (val instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) val;
                        // 处理 import
                        if (data.containsKey("class-name")) {
                            annotation.getImports().add(data.get("class-name").toString());
                            data.remove("class-name");
                        }
                        data.forEach((k, v) -> {
                            String value = String.valueOf(v).replace("quote@", "");
                            boolean isQuote = (v instanceof String) && ((String) v).startsWith("quote@");
                            annotation.addProperty(k, value, isQuote);
                        });
                    } else {
                        throw new RuntimeException("注解扩展属性（x-@***）的值一定是 {} 或 有值的map!");
                    }
                    return annotation;
                }).collect(Collectors.toList());
    }


}
