package com.example.tire_management.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tire_management.service.TireRequestService;
import com.example.tire_management.service.EmailService;

@CrossOrigin(originPatterns = "*")
@RestController
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private TireRequestService tireRequestService;

    @Autowired
    private EmailService emailService;

    /**
     * Health check endpoint to prevent 502 errors
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", new Date().toString());
        health.put("server", "Tire Management Backend");
        health.put("version", "1.0");
        
        // Check MongoDB connection
        try {
            long requestCount = tireRequestService.getTotalRequestCount();
            health.put("database", "Connected");
            health.put("totalRequests", requestCount);
        } catch (Exception e) {
            health.put("database", "Error: " + e.getMessage());
            logger.warn("Database health check failed: {}", e.getMessage());
        }
        
        // Check email service
        try {
            boolean emailReady = emailService.getMailSender() != null;
            health.put("emailService", emailReady ? "Ready" : "Not Available");
        } catch (Exception e) {
            health.put("emailService", "Error: " + e.getMessage());
            logger.warn("Email service health check failed: {}", e.getMessage());
        }
        
        logger.info("🔍 Health check performed - Status: UP");
        return ResponseEntity.ok(health);
    }
    
    /**
     * Quick ping endpoint for faster health checks
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        logger.info("🏓 Ping endpoint accessed");
        return ResponseEntity.ok("pong");
    }
    
    /**
     * Root endpoint to check if server is running
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> info = new HashMap<>();
        info.put("message", "Tire Management Backend API");
        info.put("status", "Running");
        info.put("timestamp", new Date().toString());
        info.put("endpoints", new String[]{
            "/health - Health check",
            "/ping - Quick ping",
            "/api/tire-requests - Main API"
        });
        
        logger.info("🏠 Root endpoint accessed");
        return ResponseEntity.ok(info);
    }
}