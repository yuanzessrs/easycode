package com.easycode.codegen.utils;

import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: ClassUtils
 * @Description: class封装方法
 * @Author: Mr.Zeng
 * @Date: 2021-04-24 23:04
 */
@Slf4j
public class ClassUtils {

    private ClassUtils() {
    }

    /**
     * 根据对象和字段名称，获取值
     *
     * @param target    对象
     * @param fieldName 字段名称
     * @return 目标值
     */
    public static Object getValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("查找field失败,targetClass:{},fieldName:{}", target.getClass().getName(), fieldName);
            return null;
        }
    }

}
