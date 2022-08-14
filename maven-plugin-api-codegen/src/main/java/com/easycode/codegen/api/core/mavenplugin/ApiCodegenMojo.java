package com.easycode.codegen.api.core.mavenplugin;

import com.easycode.codegen.api.core.ApiCodegenRunner;
import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.utils.Methods;
import lombok.SneakyThrows;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * @ClassName: ApiCodegenMojo
 * @Description: api生成器
 * @Author: Mr.Zeng
 * @Date: 2021-04-17 16:49
 */
@Mojo(name = "ApiCodegenMojo", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ApiCodegenMojo extends AbstractMojo {

    /**
     * session
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;
    /**
     * src路径
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private String srcDir;
    /**
     * resource路径
     */
    @Parameter(defaultValue = "${project.build.resources[0].directory}", readonly = true)
    private String resourceDir;

    @Parameter
    private GlobalConfig config;

    @Parameter
    private List<GlobalConfig> configs;

    @SneakyThrows
    @Override
    public void execute() throws MojoExecutionException {
        if (config != null) {
            run(config);
        }
        Optional.ofNullable(configs).orElse(Collections.emptyList()).forEach(this::run);
    }

    private void run(GlobalConfig config) {
        String defaultApiDirPath = resourceDir + File.separator + "api";
        config.setDefinitionPath(Methods.or(
                config.getDefinitionPath(),
                config.getApiDefineDirPath(),
                defaultApiDirPath
        ));
        config.setSrcJavaPath(Optional.ofNullable(config.getSrcJavaPath()).orElse(srcDir));
        new ApiCodegenRunner().start(config);
    }

}