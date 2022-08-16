package com.easycode.codegen.api.core.config;

import lombok.Data;

import java.util.List;

/**
 * @class-name: FormatConfig
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-16 17:33
 */
@Data
public class FormatConfig {

    private Boolean enabled;

    private String outputPath;

    private Path path;

    private Param param;

    @Data
    public static class Path {

        private Replace replace;

        @Data
        public static class Replace {

            private String source;

            private String target;

        }

    }

    @Data
    public static class Param {

        private Filter filter;

        @Data
        public static class Filter {

            private List<String> nameSelector;

        }

    }


}
