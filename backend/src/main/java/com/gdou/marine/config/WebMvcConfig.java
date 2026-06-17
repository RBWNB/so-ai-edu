package com.gdou.marine.config;

import com.gdou.marine.utils.UploadPathUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadBasePath = UploadPathUtils.resolvePathFromProjectRoot(uploadBaseDir);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(UploadPathUtils.toFileResourceLocation(uploadBasePath));
    }
}
