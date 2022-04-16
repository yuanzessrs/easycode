package com.easycode.codegen.utils;

import com.google.common.base.CaseFormat;

/**
 * @ClassName: FormatUtils
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-17 23:30
 */
public class FormatUtils {

    /**
     * 去掉字符串里面的 ` '
     *
     * @param input 输入字符串
     * @return 处理后的字符串
     */
    public static String escapeQuotes(String input) {
        return input.replaceAll("[`']", "");
    }

    public static String smallSnakeToUpperSnake(String input) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_UNDERSCORE, escapeQuotes(input));
    }

    /**
     * 下划线字符串转大驼峰
     *
     * @param snakeString 下划线字符串
     * @return 大驼峰字符串
     */
    public static String snakeToUpperCamel(String snakeString) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, escapeQuotes(snakeString));
    }

    /**
     * 下划线字符串转小驼峰
     *
     * @param snakeString 下划线字符串
     * @return 小驼峰字符串
     */
    public static String snakeToLowerCamel(String snakeString) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, escapeQuotes(snakeString));
    }

    public static String lowerCamelToUpperCamel(String snakeString) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, escapeQuotes(snakeString));
    }

}
