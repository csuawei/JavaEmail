package com.db.spring.common;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * MyBatis-Plus代码生成器
 */
public class CodeGenerator {

    // 数据库连接信息（替换为自己的配置）
    private static final String URL = "jdbc:sqlserver://localhost:1433;DatabaseName=mail_system;encrypt=true;trustServerCertificate=true";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "123456";

    // 项目基础包名（替换为自己的项目包名）
    private static final String BASE_PACKAGE = "com.db.spring";

    private static final String PROJECT_ROOT_PATH = "C:\\Users\\35402\\Desktop\\Email\\spring";

    public static void main(String[] args) {
        // 1. 数据源配置
        FastAutoGenerator.create(URL, USERNAME, PASSWORD)
                // 2. 全局配置
                .globalConfig(builder -> {
                    builder.author("Su") // 设置作者
                            .outputDir(PROJECT_ROOT_PATH + "/src/main/java")
                            .commentDate("yyyy-MM-dd") // 注释日期格式
                            .disableOpenDir(); // 生成后不打开文件夹
                })
                // 3. 包配置
                .packageConfig(builder -> {
                    builder.parent(BASE_PACKAGE) // 父包名
                            .moduleName("") // 模块名（无则空）
                            .entity("entity") // 实体类包名
                            .mapper("mapper") // Mapper接口包名
                            .service("service") // Service接口包名
                            .serviceImpl("service.impl") // Service实现类包名
                            .controller("controller") // Controller包名
                            .xml("mapper.xml") // Mapper XML文件包名（resources下）
                            .pathInfo(Collections.singletonMap(OutputFile.xml,
                                    System.getProperty("user.dir") + "/src/main/resources/mapper")); // XML文件输出路径
                })
                // 4. 策略配置
                .strategyConfig(builder -> {
                    // 只保留addInclude，移除addExclude
                    builder.addInclude("sys_user", "mail_account", "mail_folder", "mail_message",
                                    "mail_recipient", "mail_attachment", "mail_contact","mail_message_folder")
                            // 移除这一行：.addExclude("unnecessary_table")
                            // 实体类策略
                            .entityBuilder()
                            .enableLombok() // 启用Lombok注解
                            .enableTableFieldAnnotation() // 生成字段注解
                            .logicDeleteColumnName("deleted") // 逻辑删除字段（若有）
                            .versionColumnName("version") // 乐观锁字段（若有）
                            // Controller策略
                            .controllerBuilder()
                            .enableRestStyle() // 生成RestController
                            .enableHyphenStyle() // URL中驼峰转连字符（如userInfo→user-info）
                            // Service策略
                            .serviceBuilder()
                            .formatServiceFileName("%sService") // Service接口名格式（如UserService）
                            .formatServiceImplFileName("%sServiceImpl") // Service实现类名格式
                            // Mapper策略
                            .mapperBuilder()
                            .enableBaseResultMap() // 生成BaseResultMap
                            .enableBaseColumnList(); // 生成BaseColumnList
                })
                // 5. 模板引擎配置（使用Freemarker）
                .templateEngine(new FreemarkerTemplateEngine())
                // 执行生成
                .execute();
    }
}
