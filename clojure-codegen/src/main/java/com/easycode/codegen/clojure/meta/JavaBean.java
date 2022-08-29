package com.easycode.codegen.clojure.meta;

import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @class-name: JavaBean
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-29 12:03
 */
@Data
public class JavaBean {

    private Class<?> sourceClass;

    private List<JavaFiled> fields;

    public String getClassName() {
        return sourceClass.getSimpleName();
    }

    @Data
    public static class JavaFiled {

        private Field sourceField;

        private String alias;

        private Boolean enableDateFormat;

        private Boolean enableToString;

        public String getName() {
            return sourceField.getName();
        }

        public boolean hasAlias() {
            return !ObjectUtils.isEmpty(alias);
        }

    }

}
