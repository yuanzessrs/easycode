package com.easycode.codegen.api.core.util;

import com.easycode.codegen.api.core.meta.AnnotationDefinition;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * @ClassName: AnnotationUtils
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-12 23:37
 */
public class AnnotationUtils {

    public static AnnotationDefinition lombokBuilder() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("Builder");
        annotation.getImports().add("lombok.Builder");
        return annotation;
    }

    public static AnnotationDefinition lombokGetter() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("Getter");
        annotation.getImports().add("lombok.Getter");
        return annotation;
    }

    public static AnnotationDefinition lombokToString() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("ToString");
        annotation.getImports().add("lombok.ToString");
        return annotation;
    }

    public static AnnotationDefinition lombokToStringWithExclude(List<String> exclude) {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("ToString");
        annotation.getImports().add("lombok.ToString");
        annotation.addProperty("exclude", generateStringArrayVal(exclude), false);
        return annotation;
    }


    public static AnnotationDefinition notNull() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("NotNull");
        annotation.getImports().add("javax.validation.constraints.NotNull");
        return annotation;
    }

    public static List<AnnotationDefinition> jacksonPropertyOrAlias(List<String> values) {
        return Optional.ofNullable(values).map(vs -> {
            AtomicBoolean first = new AtomicBoolean(true);
            return vs.stream()
                    .map(v -> first.compareAndSet(true, false) ? jsonProperty(v) : jsonAlias(v))
                    .collect(Collectors.toList());
        }).orElse(Collections.emptyList());
    }

    public static AnnotationDefinition getParamAlias(String alias) {
        return getParamAlias(Collections.singletonList(alias));
    }

    public static AnnotationDefinition getParamAlias(List<String> aliases) {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("GetParamsAlias");
        annotation.getImports().add("com.easycode.plugins.springboot.alias.GetParamsAlias");
        annotation.addProperty(null, generateStringArrayVal(aliases), false);
        return annotation;
    }

    public static AnnotationDefinition jsonAlias(String value) {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("JsonAlias");
        annotation.getImports().add("com.fasterxml.jackson.annotation.JsonAlias");
        annotation.addProperty(null, value.trim(), true);
        return annotation;
    }

    public static AnnotationDefinition jsonProperty(String value) {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("JsonProperty");
        annotation.getImports().add("com.fasterxml.jackson.annotation.JsonProperty");
        annotation.addProperty(null, value.trim(), true);
        return annotation;
    }

    public static AnnotationDefinition jsonInclude() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("JsonInclude");
        annotation.getImports().add(Arrays.asList(
                "com.fasterxml.jackson.annotation.JsonInclude",
                "com.fasterxml.jackson.annotation.JsonInclude.Include"
        ));
        annotation.addProperty(null, "Include.NON_NULL", false);
        return annotation;
    }

    public static AnnotationDefinition jsonIgnore() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("JsonIgnoreProperties");
        annotation.getImports().add("com.fasterxml.jackson.annotation.JsonIgnoreProperties");
        annotation.addProperty("ignoreUnknown", "true", false);
        return annotation;
    }

    public static AnnotationDefinition valid() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("Valid");
        annotation.getImports().add("javax.validation.Valid");
        return annotation;
    }

    public static String generateStringArrayVal(List<String> vals) {
        //{"1","2"}
        if (CollectionUtils.isEmpty(vals)) {
            return "{}";
        } else {
            return "{" + vals.stream().map(alias -> "\"" + alias + "\"").collect(Collectors.joining(", ")) + "}";
        }
    }

}
