package com.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:${app.upload-dir:C:/w001/weixin_upload}}")
    private String uploadDir;

    @Value("${app.upload-url:/upload/}")
    private String uploadUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String url = uploadUrl;
        if (!StringUtils.hasText(url)) url = "/upload/";
        if (!url.startsWith("/")) url = "/" + url;
        if (!url.endsWith("/")) url = url + "/";

        String dir = uploadDir;
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            dir = dir + File.separator;
        }

        registry.addResourceHandler(url + "**")
                .addResourceLocations("file:" + dir);
    }
}
