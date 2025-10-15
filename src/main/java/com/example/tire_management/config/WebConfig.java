package com.example.tire_management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**") // Allow all routes including email endpoints
                .allowedOrigins(
                    "http://localhost:3001",           // Local React development
                    "http://localhost:3000",           // Alternative React port
                    "https://tire-slt.vercel.app",     // Production Vercel
                    "https://tire-frontend-main.vercel.app", // Alternative Vercel
                    "https://tire-frontend.vercel.app" // Additional Vercel domain
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true) // Enable credentials for local development
                .maxAge(3600)
                .exposedHeaders("Content-Type", "Authorization"); // Expose headers for frontend
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Configure static file serving for uploads directory
        // Use absolute path for production environment compatibility
        String uploadPath = System.getProperty("user.dir") + "/uploads/";
        System.out.println("📁 WebConfig: Configuring static resources from: " + uploadPath);
        
        registry.addResourceHandler("/static-uploads/**")
                .addResourceLocations("file:" + uploadPath)
                .setCachePeriod(3600); // Cache for 1 hour
    }
}
