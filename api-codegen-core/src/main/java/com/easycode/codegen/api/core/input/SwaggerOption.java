package com.easycode.codegen.api.core.input;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @class-name: SwaggerOption
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-09-14 15:31
 */
@Data
public class SwaggerOption {

    private Preprocess preprocess;

    private YamlToJsonConfig yamlToJson;

    private List<TypeMapping> typeMappings;

    @Data
    public static class Preprocess {

        private ConsumesAndProducesFilter consumesAndProducesFilter;

        @Data
        public static class ConsumesAndProducesFilter {

            private Set<String> patterns;

            private Boolean enabledRegex;

            public Boolean enabledRegex() {
                return Boolean.TRUE.equals(enabledRegex);
            }
        }

        private TagOption tag;

        @Data
        public static class TagOption {

            private List<TagRename> renames;

            private List<TagVendorExtensions> appendVendorExtensions;

            @Data
            public static class TagVendorExtensions {

                private String tagNamePattern;

                private Boolean enabledRegex;

                private Map<String, Object> vendorExtensions;

                public boolean enabledRegex() {
                    return Boolean.TRUE.equals(enabledRegex);
                }
            }

            @Data
            public static class TagRename {

                private String sourceName;

                private String targetName;

                private Boolean enabledRegex;

            }

            private Filter includeFilter;

            private Filter excludeFilter;

            public boolean hashFilter() {
                return includeFilter != null || excludeFilter != null;
            }

            @Data
            public static class Filter {

                private Set<String> byNames;

            }

        }

        private OperationOption operation;

        @Data
        public static class OperationOption {

            private List<OperationRequiredQueryParams> requiredQueryParams;

            @Data
            public static class OperationRequiredQueryParams {

                private String url;

                private String httpMethod;

                private Set<String> paramNames;

                private Boolean enabledRegex;


            }

            private List<OperationIdRewrite> idRewrites;

            @Data
            public static class OperationIdRewrite {

                private String limitedTag;

                private String originalId;

                private String targetId;

                private Boolean enabledRegex;

            }

            private List<OperationConsumeRewrite> consumeRewrites;

            @Data
            public static class OperationConsumeRewrite {

                private String url;

                private String httpMethod;

                private String consume;

                private Boolean clearFlag;

                private Boolean enabledRegex;

            }

            private Filter includeFilter;

            private Filter excludeFilter;

            public boolean hashFilter() {
                return includeFilter != null || excludeFilter != null;
            }

            @Data
            public static class Filter {

                private Set<String> byIds;

                private Set<String> byUrls;

            }

        }

        private RefOption ref;

        @Data
        public static class RefOption {

            private List<RefRewrite> rewrites;

            @Data
            public static class RefRewrite {

                private String originalRef;

                private String targetRef;

                private List<String> imports;

                private Boolean enabledRegex;

            }

        }

        private DefinitionOption definition;

        @Data
        public static class DefinitionOption {

//            private List<DefinitionRename> renames;
//
//            @Data
//            public static class DefinitionRename {
//
//                private String sourceName;
//
//                private String targetName;
//
//                private Boolean enabledRegex;
//
//            }


            private Filter includeFilter;

            private Filter excludeFilter;

            public boolean hashFilter() {
                return includeFilter != null || excludeFilter != null;
            }

            @Data
            public static class Filter {

                private Set<String> byNames;

            }

        }


    }


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

        private List<String> imports;

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
