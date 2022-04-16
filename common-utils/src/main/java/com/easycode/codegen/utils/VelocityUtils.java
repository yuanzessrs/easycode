package com.easycode.codegen.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * @ClassName: VelocityUtils
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-19 17:30
 */
public class VelocityUtils {

    /**
     * 渲染模板
     *
     * @param vmPath     模板路径
     * @param params     上下文参数
     * @param targetFile 目标写入文件
     */
    public static void render(String vmPath, Map<String, Object> params, File targetFile) {
        String content = parse(vmPath, params);
        try {
            FileUtils.forceMkdirParent(targetFile);
            FileUtils.write(targetFile, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据上下文参数和模板文件渲染返回字符串
     *
     * @param vmPath        模板路径
     * @param contextParams 上下文参数
     * @return 渲染结果
     */
    public static String parse(String vmPath, Map<String, Object> contextParams) {
        // 初始化模板引擎
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        // 获取模板文件
        Template template = velocityEngine.getTemplate(vmPath);
        // 设置变量，velocityContext是一个类似map的结构
        VelocityContext velocityContext = new VelocityContext();
        contextParams.forEach(velocityContext::put);
        // 输出渲染后的结果
        StringWriter stringWriter = new StringWriter();
        template.merge(velocityContext, stringWriter);

        return stringWriter.toString().replace("\r\n", "\n");
    }
}
