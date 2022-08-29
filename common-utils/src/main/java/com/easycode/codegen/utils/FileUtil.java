package com.easycode.codegen.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @class-name: FileUtil
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-13 10:35
 */
public class FileUtil {

    private FileUtil() {
    }

    public static List<File> parse(String path) {
        return parse(new File(path));
    }

    private static List<File> parse(File file) {
        if (file == null || !file.exists()) {
            return Collections.emptyList();
        }
        if (file.isDirectory()) {
            return Optional.ofNullable(file.listFiles()).map(Arrays::stream).orElse(Stream.empty())
                    .flatMap(f -> FileUtil.parse(f).stream())
                    .collect(Collectors.toList());
        }
        return Collections.singletonList(file);
    }

}
