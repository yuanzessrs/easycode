package com.easycode.codegen.api.core.config.plugin;

import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @class-name: DtoStringFieldCheckPlugin
 * @description:
 * @author: Mr.Zeng
 * @date: 2022/3/26 18:18
 */
@Data
public class DtoStringFieldCheckPlugin {

    private String field;
    private List<String> fields;
    private List<DtoStringFieldCheckPlugin.FilterAnnotation> filterByAnnotations;

    @Data
    public static class FilterAnnotation {

        private String name;
        private List<DtoStringFieldCheckPlugin.FilterAnnotation.Property> properties;

        public FilterAnnotation() {
        }

        public String toString() {
            return ObjectUtils.isEmpty(this.properties) ? "@" + this.name
                    : "@" + this.name + "(" + this.properties.stream().map(Property::toString).collect(Collectors.joining(", ")) + ")";
        }

        @Data
        public static class Property {

            private String key;
            private String value;
            private boolean enabledQuotes;

            public String toString() {
                if (this.key != null && !"".equals(this.key.trim())) {
                    String format = this.enabledQuotes ? "%s = \"%s\"" : "%s = %s";
                    return String.format(format, this.key, this.value);
                } else {
                    return this.enabledQuotes ? "\"" + this.value + "\"" : this.value;
                }
            }
        }
    }
}

