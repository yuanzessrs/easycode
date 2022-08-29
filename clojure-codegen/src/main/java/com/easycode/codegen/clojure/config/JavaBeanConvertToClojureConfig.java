package com.easycode.codegen.clojure.config;

import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.List;

/**
 * @class-name: JavaBeanConvertToClojureConfig
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-26 18:01
 */
@Data
public class JavaBeanConvertToClojureConfig {

    private String srcPath;

    private String beanPackage;

    private String clojurePackage;

    private List<MavenDependence> compileDependencies;

    private Boolean enabledJacksonSupport = true;

    public String getBeanPath() {
        return ObjectUtils.isEmpty(beanPackage)
                ? srcPath
                : srcPath + File.separator + beanPackage.replaceAll("\\.", File.separator);
    }



}
