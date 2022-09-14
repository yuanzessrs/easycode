package com.easycode.codegen.api.core.input;

import lombok.Data;

import java.util.List;

/**
 * @class-name: SwaggerOption
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-09-14 15:31
 */
@Data
public class SwaggerOption {

    private YamlToJsonConfig yamlToJson;

    private List<TypeMapping> swaggerTypeMappings;

    /**
     * @class-name: SwaggerTypeMapping
     * @description:
     * @author: Mr.Zeng
     * @date: 2022-09-14 15:28
     */
    @Data
    public static class TypeMapping {

        private String swaggerType;

        private String swaggerFormat;

        private String javaType;

        private String javaSubtype;

    }

    /**
     * @class-name: YamlToJsonConfig
     * @description:
     * @author: Mr.Zeng
     * @date: 2022-08-16 17:33
     */
    @Data
    public static class YamlToJsonConfig {

        private Boolean enabled;

        private String outputPath;

        private Path path;

        private Param param;

        @Data
        public static class Path {

            private Path.Replace replace;

            @Data
            public static class Replace {

                private String source;

                private String target;

            }

        }

        @Data
        public static class Param {

            private Param.Filter filter;

            @Data
            public static class Filter {

                private List<String> nameSelector;

            }

        }


    }

}
