package com.easycode.codegen.sql.mavenplugin;

import com.easycode.codegen.sql.core.SqlCodegenRunner;
import com.easycode.codegen.sql.core.config.GlobalConfig;
import com.easycode.codegen.sql.core.enumuration.DB;
import com.easycode.codegen.sql.core.enumuration.ORM;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


/**
 * @ClassName: SqlToEntityConvertorMojo
 * @Description: 通过sql生成entity，sql「mysql」 entity「mybatis-plus」，后续支持其他操作
 * @Author: Mr.Zeng
 * @Date: 2021-04-17 16:49
 */
@Mojo(name = "SqlToEntityConvertor", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SqlToEntityConvertorMojo extends AbstractMojo {

    /**
     * session
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;
    /**
     * pom
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
    /**
     * 项目根目录
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;
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
    /**
     * target路径
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String targetDirectory;

    @Parameter(required = false)
    private GlobalConfig config;

    @Parameter(required = false)
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
        // 检查 sqlFilePath 、 srcPath 注入默认值
        config.setSqlFilePath(Optional.ofNullable(config.getSqlFilePath())
                .orElse(resourceDir + File.separator + "db" + File.separator + "TableSchema.sql"));
        config.setSrcJavaPath(Optional.ofNullable(config.getSrcJavaPath()).orElse(srcDir));
        config.setDbName(Optional.ofNullable(config.getDbName()).orElse(DB.MYSQL.getId()));
        config.setOrmName(Optional.ofNullable(config.getOrmName()).orElse(ORM.MYBATIS_PLUS.getId()));
        config.setEntitySuffix(Optional.ofNullable(config.getEntitySuffix()).orElse("DO"));
        config.setEntityPackageName(Optional.ofNullable(config.getEntityPackageName()).orElse("entities"));
        new SqlCodegenRunner().start(config);
    }

}