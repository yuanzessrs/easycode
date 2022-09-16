package com.easycode.codegen.api.core.components;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @class-name: SwaggerTypeConvertor
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-09-14 16:06
 */
@Data
public class SwaggerTypeConvertor {

    private static final List<Mapping> MAPPINGS = Arrays.asList(
            Mapping.of("string", String.class, false),
            Mapping.of("integer", Integer.class, false),
            Mapping.of("integer", "int32", Integer.class, false),
            Mapping.of("integer", "int64", Long.class, false),
            Mapping.of("number", "float", Float.class, false),
            Mapping.of("number", "double", Double.class, false),

            Mapping.of("number", BigDecimal.class),
            Mapping.of("string", "byte", Byte.class, false),
            Mapping.of("string", "date", LocalDate.class),
            Mapping.of("string", "date-time", LocalDateTime.class),
            Mapping.of("boolean", Boolean.class, false)
    );

    private final Map<String, Mapping> map = new HashMap<>();

    {
        MAPPINGS.forEach(this::register);
    }


    public void register(Mapping mapping) {
        map.put(toPattern(mapping.getType(), mapping.getFormat()), mapping);
    }

    public JavaType convert(String type, String format) {
        return Optional.ofNullable(map.get(toPattern(type, format)))
                .map(Mapping::getJavaType)
                .orElseThrow(() -> new RuntimeException(String.format("Unknown or UnSupport Type:%s, Format:%s", type, format)));
    }

    static String toPattern(String type, String format) {
        return StringUtils.isBlank(format) ? type : type + "@" + format;
    }

    @Builder
    @Getter
    public static class Mapping {

        private String type;

        private String format;

        private JavaType javaType;

        public static Mapping of(String swaggerType, Class<?> javaClass) {
            return of(swaggerType, javaClass, true);
        }

        public static Mapping of(String swaggerType, Class<?> javaClass, boolean enableImport) {
            return of(swaggerType, null, javaClass, enableImport);
        }

        public static Mapping of(String swaggerType, String swaggerFormat, Class<?> javaClass) {
            return of(swaggerType, swaggerFormat, javaClass, true);
        }

        public static Mapping of(String swaggerType, String swaggerFormat, Class<?> javaClass, boolean enableImport) {
            return Mapping.builder()
                    .type(swaggerType)
                    .format(swaggerFormat)
                    .javaType(JavaType.of(javaClass, enableImport))
                    .build();
        }

    }


    @Builder
    public static class JavaType {

        private String type;

        private String subtype;

        private List<String> imports;

        public List<String> imports() {
            return Optional.ofNullable(imports).orElse(Collections.emptyList());
        }

        public static JavaType of(Class<?> clazz, boolean enableImport) {
            return JavaType.builder()
                    .type(clazz.getSimpleName())
                    .imports(enableImport ? Collections.singletonList(clazz.getName()) : Collections.emptyList())
                    .build();
        }

        public String formatType() {
            return ObjectUtils.isEmpty(subtype) ? type : type + "<" + subtype + ">";
        }

    }


}
