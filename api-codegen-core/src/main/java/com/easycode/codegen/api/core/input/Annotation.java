package com.easycode.codegen.api.core.input;

import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @class-name: Annotation
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-14 21:31
 */
@Data
public class Annotation {

    private String name;

    private List<String> imports;

    private List<Property> properties;

    public Annotation() {
    }

    public String toString() {
        return ObjectUtils.isEmpty(this.properties) ? "@" + this.name
                : "@" + this.name + "(" + this.properties.stream().map(Property::toString).collect(Collectors.joining(", ")) + ")";
    }

    @Data
    public static class Property {

        private String key;

        private String value;

        private List<String> values;

        private boolean enabledQuotes;

        public String toString() {
            return generateKeyPrefix() + generateValueOutput();
        }

        private String generateKeyPrefix() {
            return (this.key != null && !"".equals(this.key.trim())) ? key + " = " : "";
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
