package com.amrita.amritabackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dfux6yum5", // 🔁 replace with your cloud name
                "api_key", "571827322221553",
                "api_secret", "U8qSIFJm1BI6BnZEZszNqLw2jUg"));
    }
}
