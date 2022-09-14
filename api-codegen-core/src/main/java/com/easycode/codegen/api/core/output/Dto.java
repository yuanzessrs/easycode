package com.easycode.codegen.api.core.output;

import com.easycode.codegen.api.core.holders.DataHolder;
import com.easycode.codegen.utils.FormatUtils;
import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @ClassName: Dto
 * @Description: dto定义
 * @Author: Mr.Zeng
 * @Date: 2021-04-23 17:35
 */
@Data
public class Dto implements Importable {

    private String name;

    private String description;

    private String overrideToString;

    private Boolean isGetParamsDTO = Boolean.FALSE;

    private Boolean hasBuilder = Boolean.FALSE;

    private final DataHolder<String> imports = new DataHolder<>();

    private final DataHolder<AnnotationDefinition> annotations = new DataHolder<>();

    private List<Field> fields = new ArrayList<>();

    @Override
    public List<String> getExternalImports() {
        List<String> externalImports = new ArrayList<>(imports.get());

        fields.forEach(field -> {
            externalImports.addAll(field.getImports().get());
            field.getAnnotations().get()
                    .forEach(annotation -> externalImports.addAll(annotation.getImports().get()));
        });
        annotations.get().forEach(annotation -> externalImports.addAll(annotation.getImports().get()));
        return externalImports.stream().distinct().collect(Collectors.toList());
    }

    public List<Dto> getInnerDtos() {
        return fields.stream()
                .sorted(Comparator.comparing(Field::getIndex))
                .map(Field::getDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Data
    public static class Field {

        public static final Comparator<Field> COMPARATOR = Comparator.comparing(Field::getIndex);

        private Dto dto;

        private String type;

        private String name;

        private Integer index;

        private List<String> aliasValues;

        private String value;

        private String description;

        private boolean readOnly;

        private final DataHolder<String> imports = new DataHolder<>();

        private final DataHolder<AnnotationDefinition> annotations = new DataHolder<>();

        public boolean needQuote() {
            if (Double.class.getSimpleName().equals(type)
                    || BigDecimal.class.getSimpleName().equals(type)
                    || Boolean.class.getSimpleName().equals(type)
                    || Integer.class.getSimpleName().equals(type)
                    || Float.class.getSimpleName().equals(type)
                    || Long.class.getSimpleName().equals(type)) {
                return false;
            }
            return true;
        }

        /**
         * @return format 默认值
         */
        public String value() {
            if (ObjectUtils.isEmpty(value)) {
                return "";
            }
            String newValue = String.class.getSimpleName().equals(type) ? "\"" + value + "\"" : value;
            newValue = " = " + newValue;
            if (Double.class.getSimpleName().equals(type)) {
                newValue = newValue + "D";
            }
            if (Long.class.getSimpleName().equals(type)) {
                newValue = newValue + "L";
            }
            return newValue;
        }

        public void setType(String type) {
            this.type = type;
        }

        /**
         * dto渲染使用
         *
         * @return upperCamelCaseName
         */
        public String upperCamelCaseName() {
            return FormatUtils.lowerCamelToUpperCamel(name);
        }

    }

}
