package com.example.tire_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class TireManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TireManagementApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/tire_orders")
                        .allowedOrigins("http://localhost:3001")
                        .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }


}
