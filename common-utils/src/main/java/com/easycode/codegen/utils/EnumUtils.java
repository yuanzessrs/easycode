package com.easycode.codegen.utils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: EnumUtils
 * @Description: 枚举工具类
 * @Author: Mr.Zeng
 * @Date: 2021-04-19 11:49
 */
@Slf4j
public class EnumUtils {

    private EnumUtils() {

    }

    /**
     * 通用枚举查找方法
     *
     * @param enumClass 枚举类
     * @param target    目标值
     * @param <T>       泛型类
     * @return 匹配的枚举
     */
    @SneakyThrows
    public static <T extends Enum<T>> T getEnum(Class<T> enumClass, Object target) {
        Objects.requireNonNull(target, "target value must not be null!");
        Field field = enumClass.getDeclaredField("id");
        field.setAccessible(true);
        Objects.requireNonNull(field, "enum class must contains an id field");
        return Arrays.stream(enumClass.getEnumConstants()).filter(o -> {
            try {
                return field.get(o).equals(target);
            } catch (IllegalAccessException e) {
                log.error("matching failure!", e);
                return false;
            }
        }).findFirst().orElseThrow(() -> new RuntimeException(
                "no matching enum value! if the id field is a custom type, you need to redefine the hashcode and equals methods"));
    }

}
