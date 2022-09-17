package com.easycode.codegen.api.core.input;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @class-name: CodegenCustom
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-01-03 13:21
 */
@Data
public class CodegenCustom {

    private AutoImport autoImport;

    private DTO dto;

    @Data
    public static class AutoImport {

        private Map<String, String> mappings;

    }

    @Data
    public static class DTO {

        private List<Annotation> presetAnnotations;

        private Field field;

        private ToString toString;

        private Builder builder;

        private Boolean enabledLombokGetter;

        private Boolean enabledLombokSetter;


        @Data
        public static class Field {

            private Boolean autoRenameToLowerCamel;

        }

    }

    @Data
    public static class Builder {

        private Lombok lombok;

        @Data
        public static class Lombok {

        }

    }

    @Data
    public static class ToString {

        private Lombok lombok;

        private Custom custom;

        @Data
        public static class Lombok {

            private String excludeField;
            private List<String> excludeFields;
        }

        @Data
        public static class Custom {

            private String extImport;
            private String template;

        }

    }


}
