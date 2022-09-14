package com.easycode.codegen.api.core.output;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;

/**
 * @InterfaceName: Importable
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-24 23:16
 */
public interface Importable {

    /**
     * 实现者处理内部以来，返回需要导入的class
     *
     * @return 需要导入的class
     */
    List<String> getExternalImports();

    /**
     * 根据返回的需要导入的class列表，进行加工处理，过滤掉不合法的项
     *
     * @return 需要导入的class（处理后）
     */
    default List<String> getExternalImportsWithFilter() {
        return getExternalImports()
                .stream()
                .filter(o -> !ObjectUtils.isEmpty(o))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

    }

}
