package com.easycode.codegen.api.core.input.plugin;

import com.easycode.codegen.api.core.input.Annotation;
import lombok.Data;

import java.util.List;

/**
 * @class-name: DtoStringFieldCheckPlugin
 * @description:
 * @author: Mr.Zeng
 * @date: 2022/3/26 18:18
 */
@Data
public class DtoStringFieldCheckPlugin {

    private String field;

    private List<String> fields;

    private List<Annotation> filterByAnnotations;

}

