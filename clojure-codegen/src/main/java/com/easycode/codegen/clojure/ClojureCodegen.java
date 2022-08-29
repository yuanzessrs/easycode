package com.easycode.codegen.clojure;

import com.easycode.codegen.clojure.config.JavaBeanConvertToClojureConfig;
import com.easycode.codegen.clojure.config.MavenDependence;
import com.easycode.codegen.clojure.meta.JavaBean;
import com.easycode.codegen.utils.ClassUtils;
import com.easycode.codegen.utils.FileUtil;
import com.easycode.codegen.utils.Maven;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @class-name: ClojureCodegen
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-26 18:00
 */
public class ClojureCodegen {

    public static void main(String[] args) {
        List<MavenDependence> mavenDependencies = Arrays.asList(
                new MavenDependence("org.projectlombok", "lombok", "1.14.2"),
                new MavenDependence("com.fasterxml.jackson.core", "jackson-annotations", "2.13.2"),
                new MavenDependence("com.fasterxml.jackson.core", "jackson-databind", "2.13.2.2")
        );
        JavaBeanConvertToClojureConfig config = new JavaBeanConvertToClojureConfig();
        config.setSrcPath("/Users/didi/company-code/easycode/clojure-codegen/src/test/tmp-project");
        config.setBeanPackage("com.java.demo");
        config.setCompileDependencies(mavenDependencies);
        run(config);
    }

    public static void run(JavaBeanConvertToClojureConfig config) {
        prepareCompileDependence(config.getCompileDependencies());
        List<JavaBean> beans = scanJavaBeans(config);
    }

    public static void prepareCompileDependence(List<MavenDependence> dependencies) {
        Optional.ofNullable(dependencies).orElse(Collections.emptyList())
                .forEach(dep -> Maven.pullJar(dep.getGroupId(), dep.getArtifactId(), dep.getVersion()));
    }


    public static List<JavaBean> scanJavaBeans(JavaBeanConvertToClojureConfig config) {
        return FileUtil.parse(config.getBeanPath())
                .stream()
                .filter(file -> file.getName().endsWith(".java"))
                .map(file -> {
                    JavaBean bean = new JavaBean();
                    ClassUtils.javac(file.getAbsolutePath());
                    String classFullName = config.getBeanPackage() + "." + file.getName().replace(".java", "");
                    try {
                        bean.setSourceClass(Class.forName(classFullName));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    return bean;
                }).collect(Collectors.toList());
    }

    static String generateCpParams(List<MavenDependence> dependencies) {
        return dependencies.stream()
                .map(dependence -> Maven.genJarPath(dependence.getGroupId(), dependence.getArtifactId(), dependence.getVersion()))
                .collect(Collectors.joining(":"));
    }

}
