package com.easycode.codegen.api.core.meta;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;

/**
 * @interface-name: HandlerImportable
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-01-12 17:52
 */
public interface HandlerImportable {

    List<String> getOutputControllerImports();

    List<String> getOutputFeignClientImports();

    List<String> getOutputServiceImports();

    default List<String> getControllerImportsWithFilter() {
        return processImports(getOutputControllerImports());
    }

    default List<String> getFeignClientImportsWithFilter() {
        return processImports(getOutputFeignClientImports());
    }

    default List<String> getServiceImportsWithFilter() {
        return processImports(getOutputServiceImports());
    }

    default List<String> processImports(List<String> inputImports) {
        return inputImports
                .stream()
                .filter(o -> !ObjectUtils.isEmpty(o))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }


}
