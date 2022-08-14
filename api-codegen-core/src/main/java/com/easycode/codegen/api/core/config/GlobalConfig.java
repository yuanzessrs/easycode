package com.easycode.codegen.api.core.config;

import lombok.Data;

/**
 * @ClassName: GlobalConfig
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-25 00:16
 */
@Data
public class GlobalConfig {

    /**
     * 生成类型（springmvc,feignClient）
     */
    private String generateType = "SpringMvc";

    /**
     * swagger文档目录 @后续使用definitionPath
     *
     */
    @Deprecated
    private String apiDefineDirPath;


    private String definitionPath;

    /**
     * 项目src目录的路径(默认可以从pom取，如果要生成在其他地方可自定义)
     */
    private String srcJavaPath;
    /**
     * 项目root包(Application类所在包路径)
     */
    private String basePackage;

    private String controllerPackage;

    private String servicePackage;

    private String dtoPackage;

    private String feignClientPackage;

    /**
     * controller包名
     */
    private String controllerPackageName = "controllers";
    /**
     * service包名
     */
    private String servicePackageName = "services";
    /**
     * dto包名
     */
    private String dtoPackageName = "dtos";

    /**
     * feignClient包名
     */
    private String feignClientPackageName = "clients";

    private CustomConfig custom;

    private Plugins plugins;

}
