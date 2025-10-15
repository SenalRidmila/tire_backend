package com.tiremanagement.controller;

import com.tiremanagement.service.SimpleAutoEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auto-email")
@CrossOrigin(origins = {"https://tire-frontend-main.vercel.app", "https://tire-slt.vercel.app", "http://localhost:3000"})
public class AutoEmailController {
    
    private static final Logger logger = LoggerFactory.getLogger(AutoEmailController.class);
    
    @Autowired
    private SimpleAutoEmailService autoEmailService;
    
    // Test Complete Workflow
    @PostMapping("/test-workflow")
    public ResponseEntity<?> testCompleteWorkflow(@RequestBody Map<String, String> request) {
        try {
            String testEmail = request.get("testEmail");
            if (testEmail == null || testEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Test email is required"
                ));
            }
            
            logger.info("🧪 Starting complete email workflow test for: " + testEmail);
            boolean success = autoEmailService.testCompleteWorkflow(testEmail);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? 
                "✅ Complete workflow test successful! Check all email inboxes for 4 test emails." :
                "❌ Workflow test failed. Check logs for details.");
            response.put("testEmail", testEmail);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Workflow test endpoint error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Workflow test failed: " + e.getMessage()
            ));
        }
    }
    
    // Test Single Manager Email
    @PostMapping("/test-manager")
    public ResponseEntity<?> testManagerEmail() {
        try {
            logger.info("🧪 Testing manager email notification...");
            boolean success = autoEmailService.sendManagerNotification(null); // null triggers test data
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? 
                    "✅ Manager email test successful!" :
                    "❌ Manager email test failed. Check logs.",
                "emailType", "manager",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("❌ Manager email test error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Manager email test failed: " + e.getMessage()
            ));
        }
    }
    
    // Test TTO Email
    @PostMapping("/test-tto")
    public ResponseEntity<?> testTTOEmail() {
        try {
            logger.info("🧪 Testing TTO email notification...");
            boolean success = autoEmailService.sendTTONotification(null);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? 
                    "✅ TTO email test successful!" :
                    "❌ TTO email test failed. Check logs.",
                "emailType", "tto",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("❌ TTO email test error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "TTO email test failed: " + e.getMessage()
            ));
        }
    }
    
    // Test Engineer Email
    @PostMapping("/test-engineer")
    public ResponseEntity<?> testEngineerEmail() {
        try {
            logger.info("🧪 Testing engineer email notification...");
            boolean success = autoEmailService.sendEngineerNotification(null);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? 
                    "✅ Engineer email test successful!" :
                    "❌ Engineer email test failed. Check logs.",
                "emailType", "engineer",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("❌ Engineer email test error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Engineer email test failed: " + e.getMessage()
            ));
        }
    }
    
    // Test User Final Email
    @PostMapping("/test-user")
    public ResponseEntity<?> testUserFinalEmail(@RequestBody Map<String, String> request) {
        try {
            String testEmail = request.get("testEmail");
            if (testEmail == null || testEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Test email is required"
                ));
            }
            
            logger.info("🧪 Testing user final email notification for: " + testEmail);
            boolean success = autoEmailService.sendUserFinalNotification(null, testEmail);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? 
                    "✅ User final email test successful!" :
                    "❌ User final email test failed. Check logs.",
                "emailType", "user_final",
                "testEmail", testEmail,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("❌ User final email test error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "User final email test failed: " + e.getMessage()
            ));
        }
    }
    
    // Get Email Configuration Status
    @GetMapping("/config-status")
    public ResponseEntity<?> getConfigurationStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("service", "AutoEmailService");
            status.put("status", "active");
            status.put("emailTypes", new String[]{"manager", "tto", "engineer", "user_final"});
            status.put("features", new String[]{"HTML emails", "Simple text fallback", "Complete workflow testing"});
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Configuration status error: " + e.getMessage()
            ));
        }
    }
    
    // Health Check for Email Service
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("service", "AutoEmailService");
            health.put("status", "healthy");
            health.put("description", "Pure Java backend email service - No external dependencies");
            health.put("render_compatible", true);
            health.put("cloud_ready", true);
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "service", "AutoEmailService",
                "status", "unhealthy",
                "error", e.getMessage()
            ));
        }
    }
}