package com.easycode.codegen.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;

/**
 * @class-name: Terminal
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-29 16:29
 */
@Slf4j
public class Terminal {

    @SneakyThrows
    public static void exec(String command) {
        log.info("exec command=> {}", command);
        Process process = Runtime.getRuntime().exec(command);
        int ok = process.waitFor();
        SequenceInputStream mergeStream = new SequenceInputStream(process.getInputStream(), process.getErrorStream());
        log.info(IOUtils.toString(mergeStream, StandardCharsets.UTF_8));
        if (ok != 0) {
            throw new RuntimeException("exec command fail. cmd: " + command);
        }
    }

}
