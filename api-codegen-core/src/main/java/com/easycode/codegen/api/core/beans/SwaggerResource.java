package com.easycode.codegen.api.core.beans;

import io.swagger.models.Swagger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * @class-name: SwaggerResource
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-09-14 15:43
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwaggerResource {

    private Swagger model;

    private File file;

}
