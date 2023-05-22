package com.easycode.codegen.api.core.util;

import com.easycode.codegen.api.core.constants.GlobalConstants;
import com.easycode.codegen.api.core.output.AnnotationDefinition;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SwaggerVendorExtensions {

    public static final String X_IMPORT = "x-import";

    public static String getXFieldVal(Map<String, Object> vendorExtensions, String key) {
        return Optional.ofNullable(vendorExtensions)
                .map(map -> map.get("x-" + key))
                .map(Object::toString)
                .orElse(null);
    }

    public static Map<String, String> getRenameMap(Map<String, Object> vendorExtensions) {
        Object renameVal = Optional.ofNullable(vendorExtensions.get("x-field-alias-map"))
                .orElseGet(() -> vendorExtensions.get("x-rename-map"));
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

    public static Optional<String> getOptionalRename(Map<String, Object> vendorExtensions) {
        return Optional.ofNullable(getRenameVal(vendorExtensions))
                .filter(val -> !ObjectUtils.isEmpty(val));
    }

    public static boolean isIgnore(Map<String, Object> vendorExtensions) {
        return "true".equalsIgnoreCase(getXFieldVal(vendorExtensions, "ignore"))
                || "true".equalsIgnoreCase(getXFieldVal(vendorExtensions, "disabled"));
    }

    public static boolean isSkipRegisteringBean(Map<String, Object> v) {
        return "true".equalsIgnoreCase(getXFieldVal(v, "skip-registering-bean"));
    }

    public static Optional<Boolean> getOptionalDisabledMergeQueryParam(Map<String, Object> vendorExtensions) {
        return Optional.ofNullable(getXFieldVal(vendorExtensions, "disabledMergeQueryParam"))
                .map("true"::equalsIgnoreCase);
    }

    public static List<String> getImports(Map<String, Object> vendorExtensions) {
        Object it = vendorExtensions.get(X_IMPORT);
        if (ObjectUtils.isEmpty(it)) {
            return Collections.emptyList();
        }
        if (it instanceof Collection) {
            return ((Collection<?>) it).stream().filter(Objects::nonNull).map(o -> (String) o).map(String::trim).collect(Collectors.toList());
        }
        return Arrays.stream(((String) it).split(GlobalConstants.SPLIT_REGEX))
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

    public static String getFeignClientName(Map<String, Object> vendorExtensions) {
        return getXFieldVal(vendorExtensions, "FeignClientName");
    }

    public static String getFeignClientContextId(Map<String, Object> vendorExtensions) {
        return getXFieldVal(vendorExtensions, "FeignClientContextId");
    }

    public static String getFeignClientPath(Map<String, Object> vendorExtensions) {
        return getXFieldVal(vendorExtensions, "FeignClientPath");
    }

    public static boolean disableFeignClientPathQuotes(Map<String, Object> vendorExtensions) {
        return "true".equalsIgnoreCase(getXFieldVal(vendorExtensions, "FeignClientDisablePathQuotes"));
    }

    public static String getPackage(Map<String, Object> vendorExtensions) {
        return getXFieldVal(vendorExtensions, "package");
    }

}
