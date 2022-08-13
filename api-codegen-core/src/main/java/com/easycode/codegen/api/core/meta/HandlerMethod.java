package com.easycode.codegen.api.core.meta;

import com.easycode.codegen.api.core.constants.HandlerMethodParamTag;
import com.easycode.codegen.api.core.holders.DataHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * @ClassName: HandlerMethod
 * @Description: 接口定义
 * @Author: Mr.Zeng
 * @Date: 2021-04-19 15:15
 */
@Data
public class HandlerMethod implements HandlerImportable {

    /**
     * 请求类型  GET POST PUT PATCH DELETE
     */
    private String requestType;

    /**
     * 对应方法名
     */
    private String methodName;

    /**
     * 接口url
     */
    private String url;

    /**
     * 接口摘要
     */
    private String summary;

    /**
     * 接口描述
     */
    private String description;

    /**
     * 接收类型
     */
    private List<String> consumes;

    /**
     * 输出类型
     */
    private List<String> produces;

    /**
     * 方法参数
     */
    List<Param> handlerMethodParams;

    /**
     * 返回数据类型
     */
    private Return handlerMethodReturn;

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

    /**
     * @return 是否开启json序列化
     */
    public boolean enableResponseBody() {
        return produces.stream().anyMatch(o -> o.contains("json"));
    }

    /**
     * @return 是否开json反序列化
     */
    public boolean enableRequestBody() {
        return consumes.stream().anyMatch(o -> o.contains("json"));
    }

    protected List<String> commonImports() {
        List<String> commonImports = new ArrayList<>(imports.get());
        annotations.get().forEach(annotation -> commonImports.addAll(annotation.getImports().get()));

        // 有问题 ?? todo
        handlerMethodParams.forEach(param -> commonImports.addAll(param.getImports().get()));
        handlerMethodParams.forEach(param -> param.getAnnotations().get()
                .forEach(annotation -> commonImports.addAll(annotation.getImports().get())));

        commonImports.addAll(handlerMethodReturn.getImports().get());
        handlerMethodReturn.getAnnotations().get()
                .forEach(annotation -> commonImports.addAll(annotation.getImports().get()));
        return commonImports;
    }

    @Override
    public List<String> getOutputControllerImports() {
        List<String> outputControllerImports = new ArrayList<>(commonImports());
        controllerAnnotations.get()
                .forEach(annotation -> outputControllerImports.addAll(annotation.getImports().get()));

        handlerMethodParams.forEach(param -> param.getControllerAnnotations().get()
                .forEach(annotation -> outputControllerImports.addAll(annotation.getImports().get())));

        return outputControllerImports;
    }

    @Override
    public List<String> getOutputFeignClientImports() {
        List<String> outputFeignClientImports = new ArrayList<>(commonImports());
        feignClientAnnotations.get()
                .forEach(annotation -> outputFeignClientImports.addAll(annotation.getImports().get()));

        handlerMethodParams.forEach(param -> param.getFeignClientAnnotations().get()
                .forEach(annotation -> outputFeignClientImports.addAll(annotation.getImports().get())));

        return outputFeignClientImports;
    }

    @Override
    public List<String> getOutputServiceImports() {
        List<String> outputServiceImports = new ArrayList<>(commonImports());
        serviceAnnotations.get().forEach(annotation -> outputServiceImports.addAll(annotation.getImports().get()));

        return outputServiceImports;
    }


    public String paramPadding() {
        int offset = handlerMethodReturn.getType().length() + methodName.length();
        StringBuilder padding = new StringBuilder();
        for (int x = 0; x < offset; x++) {
            padding.append(" ");
        }
        return padding.toString();
    }

    /**
     * @ClassName: HandlerMethodParam
     * @Description: TODO
     * @Author: Mr.Zeng
     * @Date: 2021-04-23 11:22
     */
    @Data
    public static class Param {

        /**
         * 分类{@link HandlerMethodParamTag}
         */
        private int tag;

        /**
         * 类型
         */
        private String type;

        /**
         * 名称
         */
        private String name;

        /**
         * 描述
         */
        private String description;

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

    }

    /**
     * @ClassName: HandlerMethodReturn
     * @Description: TODO
     * @Author: Mr.Zeng
     * @Date: 2021-04-23 17:34
     */
    @Data
    public static class Return {

        /**
         * 类型
         */
        private String type;

        /**
         * 描述
         */
        private String description;

        /**
         * @return 是否有返回值
         */
        public boolean hasReturn() {
            return !void.class.getSimpleName().equals(type);
        }

        /**
         * import holder
         */
        private final DataHolder<String> imports = new DataHolder<>();

        /**
         * annotation holder
         */
        private final DataHolder<AnnotationDefinition> annotations = new DataHolder<>();

    }

}
