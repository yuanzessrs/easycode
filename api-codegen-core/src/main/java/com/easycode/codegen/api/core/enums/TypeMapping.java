package com.easycode.codegen.api.core.enums;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import lombok.Getter;
import org.springframework.util.ObjectUtils;

/**
 * @EnumName: TypeMapping
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-24 10:17
 */
@Getter
public enum TypeMapping {

    STRING("string", String.class.getSimpleName(), null),

    INTEGER("integer", Integer.class.getSimpleName(), null),

    INT32("integer@int32", Integer.class.getSimpleName(), null),

    INT64("integer@int64", Long.class.getSimpleName(), null),

    FLOAT("number@float", Float.class.getSimpleName(), null),

    DOUBLE("number@double", Double.class.getSimpleName(), null),

    NUMBER("number", BigDecimal.class),

    DATE("string@date", LocalDate.class),

    DATETIME("string@date-time", LocalDateTime.class),

    BOOLEAN("boolean", Boolean.class.getSimpleName(), null),

    ;


    private final String pattern;

    private final String type;

    private final String importValue;

    TypeMapping(String pattern, Class<?> clazz) {
        this(pattern, clazz.getSimpleName(), clazz.getName());
    }

    TypeMapping(String pattern, String type, String importValue) {
        this.pattern = pattern;
        this.type = type;
        this.importValue = importValue;
    }

    public static TypeMapping parse(String type, String format) {
        String key = ObjectUtils.isEmpty(format) ? type : type + "@" + format;
        return Arrays.stream(TypeMapping.values())
                .filter(o -> o.getPattern().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Unknown or UnSupport Type:%s, Format:%s", type, format)));
    }
}
