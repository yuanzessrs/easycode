package com.easycode.codegen.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    public static boolean hasField(Object target, String fieldName) {
        try {
            Field[] fields = target.getClass().getDeclaredFields();
            return Arrays.stream(fields).anyMatch(field -> field.getName().equals(fieldName));
        } catch (Exception ignore) {

        }
        return false;
    }

    @SneakyThrows
    public static void javac(String path) {
        Process process = Runtime.getRuntime().exec("javac -cp " + "/Users/didi/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.13.2/jackson-annotations-2.13.2.jar:/Users/didi/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.13.2.2/jackson-databind-2.13.2.2.jar:/Users/didi/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.13.2/jackson-core-2.13.2.jar:/Users/didi/.m2/repository/org/projectlombok/lombok/1.18.16/lombok-1.18.16.jar " + path);
        System.out.println(IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
        int exitVal = process.waitFor();
        System.out.println("Process exitValue: " + exitVal);
    }


}
