package com.easycode.codegen.utils;

import static com.google.common.base.CaseFormat.*;

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
        return LOWER_UNDERSCORE.to(UPPER_UNDERSCORE, escapeQuotes(input));
    }

    /**
     * 下划线字符串转大驼峰
     *
     * @param snakeString 下划线字符串
     * @return 大驼峰字符串
     */
    public static String snakeToUpperCamel(String snakeString) {
        return LOWER_UNDERSCORE.to(UPPER_CAMEL, escapeQuotes(snakeString));
    }

    /**
     * 下划线字符串转小驼峰
     *
     * @param snakeString 下划线字符串
     * @return 小驼峰字符串
     */
    public static String snakeToLowerCamel(String snakeString) {
        return LOWER_UNDERSCORE.to(LOWER_CAMEL, escapeQuotes(snakeString));
    }

    public static String lowerCamelToUpperCamel(String snakeString) {
        return LOWER_CAMEL.to(UPPER_CAMEL, escapeQuotes(snakeString));
    }


    public static String toLowerCamel(String source) {
        source = source.contains("-") ? LOWER_HYPHEN.to(LOWER_CAMEL, escapeQuotes(source)) : source;
        source = source.contains("_") ? LOWER_UNDERSCORE.to(LOWER_CAMEL, escapeQuotes(source)) : source;
        return source;
    }

}
