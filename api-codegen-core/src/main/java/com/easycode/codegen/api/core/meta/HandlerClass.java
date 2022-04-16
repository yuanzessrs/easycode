package com.easycode.codegen.api.core.meta;

import com.easycode.codegen.api.core.holders.DataHolder;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * @ClassName: HandlerClass
 * @Description: 定义了 HandlerMethod 容器
 * @Author: Mr.Zeng
 * @Date: 2021-04-19 15:15
 */
@Data
public class HandlerClass implements HandlerImportable {

    /**
     * controller 名称
     */
    private String name;
    /**
     * service名称
     */
    private String serviceName;
    /**
     * feignClient对象名称
     */
    private String feignClientName;
    /**
     * 描述
     */
    private String description;
    /**
     * 基础路径
     */
    private String basePath;
    /**
     * 当前类包含的 请求方法
     */
    private List<HandlerMethod> handlerMethods;

    /**
     * import holder
     */
    private final DataHolder<String> imports = new DataHolder<>();
    /**
     * annotation holder
     */
    private final DataHolder<AnnotationDefinition> annotations = new DataHolder<>();
    private final DataHolder<AnnotationDefinition> controllerAnnotations = new DataHolder<>();
    private final DataHolder<AnnotationDefinition> feignClientAnnotations = new DataHolder<>();
    private final DataHolder<AnnotationDefinition> serviceAnnotations = new DataHolder<>();

    protected List<String> commonImports() {
        List<String> commonImports = new ArrayList<>(imports.get());
        annotations.get().forEach(annotation -> commonImports.addAll(annotation.getImports().get()));

        handlerMethods.forEach(handlerMethod -> commonImports.addAll(handlerMethod.commonImports()));
        return commonImports;
    }

    @Override
    public List<String> getOutputControllerImports() {
        List<String> outputControllerImports = new ArrayList<>(commonImports());
        controllerAnnotations.get()
                .forEach(annotation -> outputControllerImports.addAll(annotation.getImports().get()));

        handlerMethods
                .forEach(handlerMethod -> outputControllerImports.addAll(handlerMethod.getOutputControllerImports()));
        return outputControllerImports;
    }

    @Override
    public List<String> getOutputFeignClientImports() {
        List<String> outputFeignClientImports = new ArrayList<>(commonImports());
        feignClientAnnotations.get()
                .forEach(annotation -> outputFeignClientImports.addAll(annotation.getImports().get()));

        handlerMethods
                .forEach(handlerMethod -> outputFeignClientImports.addAll(handlerMethod.getOutputFeignClientImports()));
        return outputFeignClientImports;
    }

    @Override
    public List<String> getOutputServiceImports() {
        List<String> outputServiceImports = new ArrayList<>(commonImports());
        serviceAnnotations.get().forEach(annotation -> outputServiceImports.addAll(annotation.getImports().get()));

        handlerMethods.forEach(handlerMethod -> outputServiceImports.addAll(handlerMethod.getOutputServiceImports()));
        return outputServiceImports;
    }

}
