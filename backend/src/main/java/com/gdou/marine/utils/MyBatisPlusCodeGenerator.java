package com.gdou.marine.utils;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.util.Collections;

/**
 * MyBatis-Plus FastAutoGenerator 代码生成器
 */
public class MyBatisPlusCodeGenerator {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/marine_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "123456";

    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");
        String javaOutputDir = projectPath + "/src/main/java";
        String mapperXmlOutputDir = projectPath + "/src/main/resources/mapper";

        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                .globalConfig(builder -> builder
                        .author("codex")
                        .outputDir(javaOutputDir)
                        .disableOpenDir()
                        .commentDate("yyyy-MM-dd HH:mm:ss"))
                .packageConfig(builder -> builder
                        .parent("com.gdou.marine")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, mapperXmlOutputDir)))
                .strategyConfig(builder -> builder
                        .addInclude(
                                "sys_user",
                                "sys_role",
                                "sys_user_role",
                                "species",
                                "observation_record")
                        .entityBuilder()
                            .enableFileOverride()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .idType(IdType.AUTO)
                            .logicDeleteColumnName("is_deleted")
                            .addTableFills(
                                    new Column("created_at", FieldFill.INSERT),
                                    new Column("updated_at", FieldFill.INSERT_UPDATE))
                        .mapperBuilder()
                            .enableFileOverride()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            .enableMapperAnnotation()
                        .serviceBuilder()
                            .enableFileOverride()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                        .controllerBuilder()
                            .enableFileOverride()
                            .enableRestStyle()
                            .enableHyphenStyle())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}

