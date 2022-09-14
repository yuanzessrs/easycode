package com.easycode.codegen.api.core.output;

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
        List<String> imports = new ArrayList<>(this.imports.get());
        annotations.get().forEach(annotation -> imports.addAll(annotation.getImports().get()));

        handlerMethods.forEach(handlerMethod -> imports.addAll(handlerMethod.commonImports()));
        return imports;
    }

    @Override
    public List<String> getOutputControllerImports() {
        List<String> imports = new ArrayList<>(commonImports());
        controllerAnnotations.get().forEach(annotation -> imports.addAll(annotation.getImports().get()));

        handlerMethods.forEach(handlerMethod -> imports.addAll(handlerMethod.getOutputControllerImports()));
        return imports;
    }

    @Override
    public List<String> getOutputFeignClientImports() {
        List<String> imports = new ArrayList<>(commonImports());
        feignClientAnnotations.get().forEach(annotation -> imports.addAll(annotation.getImports().get()));

        handlerMethods.forEach(handlerMethod -> imports.addAll(handlerMethod.getOutputFeignClientImports()));
        return imports;
    }

    @Override
    public List<String> getOutputServiceImports() {
        List<String> imports = new ArrayList<>(commonImports());
        serviceAnnotations.get().forEach(annotation -> imports.addAll(annotation.getImports().get()));

        handlerMethods.forEach(handlerMethod -> imports.addAll(handlerMethod.getOutputServiceImports()));
        return imports;
    }

}
