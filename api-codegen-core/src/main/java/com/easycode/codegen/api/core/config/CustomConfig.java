package com.easycode.codegen.api.core.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @class-name: CustomConfig
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-01-03 13:21
 */
@Data
public class CustomConfig {

    private Map<String, String> classMappings;

    private DTO dto;

    @Data
    public static class DTO {

        private ToString toString;

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
