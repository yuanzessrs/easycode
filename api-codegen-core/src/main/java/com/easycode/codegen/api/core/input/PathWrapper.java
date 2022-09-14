package com.easycode.codegen.api.core.input;

import java.io.File;
import java.util.Optional;

/**
 * @ClassName: PathConfig
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-13 17:44
 */
public class PathWrapper {

    private final GlobalConfig config;

    public PathWrapper(GlobalConfig config) {
        this.config = config;
    }

    /**
     * @return controller生成路径
     */
    public String getControllerPackagePath() {
        return Optional.ofNullable(processPackageToProjectPath(config.getControllerPackage()))
                .orElseGet(() -> getBasePackagePath() + config.getControllerPackageName() + File.separator);
    }

    /**
     * @return service生成路径
     */
    public String getServicePackagePath() {
        return Optional.ofNullable(processPackageToProjectPath(config.getServicePackage()))
                .orElseGet(() -> getBasePackagePath() + config.getServicePackageName() + File.separator);
    }

    /**
     * @return dto生成路径
     */
    public String getDtoPackagePath() {
        return Optional.ofNullable(processPackageToProjectPath(config.getDtoPackage()))
                .orElseGet(() -> getBasePackagePath() + config.getDtoPackageName() + File.separator);
    }

    /**
     * @return feignClient生成路径
     */
    public String getFeignClientPackagePath() {
        return Optional.ofNullable(processPackageToProjectPath(config.getFeignClientPackage()))
                .orElseGet(() -> getBasePackagePath() + config.getFeignClientPackageName() + File.separator);
    }

    public String getPackagePath(String customPkg) {
        return processPackageToProjectPath(customPkg);
    }

    /**
     * @return Application所在路径
     */
    private String getBasePackagePath() {
        return config.getSrcJavaPath()
                + File.separator
                + Optional.ofNullable(config.getBasePackage()).orElse("").replace(".", File.separator)
                + File.separator;
    }

    private String processPackageToProjectPath(String packageVal) {
        return Optional.ofNullable(packageVal).map(val -> config.getSrcJavaPath()
                + File.separator
                + packageVal.replace(".", File.separator)
                + File.separator
        ).orElse(null);
    }

}
