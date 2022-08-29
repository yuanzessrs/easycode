package com.easycode.codegen.utils;

import java.io.File;

/**
 * @class-name: Maven
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-29 16:28
 */
public class Maven {

    private static final String GET_JAR_FORMAT = "mvn dependency:get -Dartifact=%s:%s:%s";

    public static String getDefaultLocalRepoPath() {
        return PathUtil.getHome() + File.separator + ".m2" + File.separator + "repository";
    }

    public static void pullJar(String groupId, String artifactId, String version) {
        if (isExist(groupId, artifactId, version)) {
            return;
        }
        Terminal.exec(String.format(GET_JAR_FORMAT, groupId, artifactId, version));
    }

    public static boolean isExist(String groupId, String artifactId, String version) {
        return new File(genJarPath(groupId, artifactId, version)).exists();
    }

    public static String genJarPath(String groupId, String artifactId, String version) {
        return getDefaultLocalRepoPath()
                + File.separator
                + groupId.replaceAll("\\.", File.separator)
                + File.separator
                + artifactId
                + File.separator
                + artifactId + "-" + version + ".jar";
    }

}
