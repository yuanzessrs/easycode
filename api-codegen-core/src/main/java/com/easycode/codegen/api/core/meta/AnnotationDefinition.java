package com.easycode.codegen.api.core.meta;

import com.easycode.codegen.api.core.holders.DataHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.util.ObjectUtils;

/**
 * @ClassName: Annotation
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-12 23:14
 */
@Data
public class AnnotationDefinition implements Comparable<AnnotationDefinition> {

    @Override
    public int compareTo(AnnotationDefinition another) {
        return annotationName.compareTo(
                Optional.ofNullable(another).map(AnnotationDefinition::getAnnotationName).orElse("")
        );
    }

    /**
     * 注解名称
     */
    private String annotationName;

    /**
     * import holder
     */
    private final DataHolder<String> imports = new DataHolder<>();

    /**
     * 注解属性
     */
    private List<Property> properties;

    /**
     * 添加属性
     *
     * @param name          属性名
     * @param value         属性值
     * @param enabledQuotes 是否加引号
     */
    public void addProperty(String name, String value, boolean enabledQuotes) {
        Objects.requireNonNull(value, "value  must not be bull");
        Property property = new Property(name, value, enabledQuotes);
        properties = Optional.ofNullable(properties).orElse(new ArrayList<>());
        properties.add(property);
    }

    /**
     * 添加属性
     *
     * @param name          属性名
     * @param values         属性值
     * @param enabledQuotes 是否加引号
     */
    public void addProperty(String name, List<String> values, boolean enabledQuotes) {
        Objects.requireNonNull(values, "values  must not be bull");
        Property property = new Property(name, values, enabledQuotes);
        properties = Optional.ofNullable(properties).orElse(new ArrayList<>());
        properties.add(property);
    }

    @Override
    public String toString() {
        if (ObjectUtils.isEmpty(properties)) {
            return "@" + annotationName;
        }
        return "@" + annotationName + "(" +
                properties.stream().map(Property::toString).collect(Collectors.joining(", "))
                + ")";
    }

    @Data
    static class Property {

        private String name;

        private String value;

        private List<String> values;

        private boolean enabledQuotes;

        public Property(String name, String value, boolean enabledQuotes) {
            this.name = name;
            this.value = value;
            this.enabledQuotes = enabledQuotes;
        }

        public Property(String name, List<String> values, boolean enabledQuotes) {
            this.name = name;
            this.values = values;
            this.enabledQuotes = enabledQuotes;
        }

        public String toString() {
            return generateKeyPrefix() + generateValueOutput();
        }

        private String generateKeyPrefix() {
            return (this.name != null && !"".equals(this.name.trim())) ? name + " = " : "";
        }

        private String generateValueOutput() {
            return ObjectUtils.isEmpty(values) ? generateSingleValueOutput(this.value) : generateArrayValueOutput(this.values);
        }

        public String generateSingleValueOutput(String val) {
            return getPadding() + this.value + getPadding();
        }

        public String generateArrayValueOutput(List<String> vals) {
            //{"1","2"}
            return "{" + vals.stream().map(this::generateSingleValueOutput).collect(Collectors.joining(", ")) + "}";
        }

        private String getPadding() {
            return this.enabledQuotes ? "\"" : "";
        }

    }


}
