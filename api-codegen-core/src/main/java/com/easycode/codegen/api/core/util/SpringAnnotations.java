package com.easycode.codegen.api.core.util;

import com.easycode.codegen.api.core.meta.AnnotationDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.util.ObjectUtils;

/**
 * @class-name: SpringAnnotations
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-01-12 15:12
 */
public class SpringAnnotations {

    private SpringAnnotations() {
    }

    public static AnnotationDefinition RequestMapping(String value, List<String> consumes, List<String> produces,
            String method) {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("RequestMapping");
        annotation.getImports().add("org.springframework.web.bind.annotation.RequestMapping");

        annotation.addProperty("value", value.trim(), true);

        if (!ObjectUtils.isEmpty(consumes)) {
            String val = AnnotationUtils.generateStringArrayVal(consumes);
            annotation.addProperty("consumes", val, false);
        }

        if (!ObjectUtils.isEmpty(produces)) {
            String val = AnnotationUtils.generateStringArrayVal(produces);
            annotation.addProperty("produces", val, false);
        }

        method = "RequestMethod." + method;
        annotation.addProperty("method", method, false);
        annotation.getImports().add("org.springframework.web.bind.annotation.RequestMethod");
        return annotation;
    }

    public static AnnotationDefinition RequestMapping(String value) {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("RequestMapping");
        annotation.getImports().add("org.springframework.web.bind.annotation.RequestMapping");
        annotation.addProperty(null, value.trim(), true);
        return annotation;
    }

    public static AnnotationDefinition PathVariable(String value) {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("PathVariable");
        annotation.getImports().add("org.springframework.web.bind.annotation.PathVariable");
        annotation.addProperty(null, value.trim(), true);
        return annotation;
    }

    public static AnnotationDefinition RequestBody() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("RequestBody");
        annotation.getImports().add("org.springframework.web.bind.annotation.RequestBody");
        return annotation;
    }

    public static AnnotationDefinition ResponseBody() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("ResponseBody");
        annotation.getImports().add("org.springframework.web.bind.annotation.ResponseBody");
        return annotation;
    }

    public static AnnotationDefinition Controller() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("Controller");
        annotation.getImports().add("org.springframework.stereotype.Controller");
        return annotation;
    }

    public static AnnotationDefinition FeignClient(String name) {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("FeignClient");
        annotation.getImports().add("org.springframework.cloud.openfeign.FeignClient");
        annotation.addProperty("name", Optional.ofNullable(name).map(String::trim).orElse("null"), true);
        return annotation;
    }

    public static AnnotationDefinition Validated() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("Validated");
        annotation.getImports().add("org.springframework.validation.annotation.Validated");
        return annotation;
    }

    public static AnnotationDefinition Valid() {
        AnnotationDefinition annotation = new AnnotationDefinition();
        annotation.setAnnotationName("Valid");
        annotation.getImports().add("javax.validation.Valid");
        return annotation;
    }

}
