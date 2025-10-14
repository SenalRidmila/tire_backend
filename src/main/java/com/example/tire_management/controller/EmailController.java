package com.example.tire_management.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.tire_management.service.EmailService;

@CrossOrigin(originPatterns = "*")
@RestController
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    @Autowired
    private EmailService emailService;

    /**
     * Send email notification endpoint for frontend
     */
    @PostMapping("/api/send-email")
    public ResponseEntity<Map<String, Object>> sendEmail(@RequestBody Map<String, Object> emailData) {
        try {
            String to = (String) emailData.get("to");
            String subject = (String) emailData.get("subject");
            String htmlContent = (String) emailData.get("html");
            
            logger.info("📧 Received email request: to={}, subject={}", to, subject);
            
            if (to == null || subject == null || htmlContent == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Missing required email fields: to, subject, html");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Send email using existing email service
            emailService.sendHtmlEmail(to, subject, htmlContent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email sent successfully");
            logger.info("✅ Email sent successfully to: {}", to);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send email: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to send email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Alternative email endpoints for different paths
     */
    @PostMapping("/api/notifications/email")
    public ResponseEntity<Map<String, Object>> sendNotificationEmail(@RequestBody Map<String, Object> emailData) {
        logger.info("📧 Notification email endpoint accessed");
        return sendEmail(emailData);
    }
    
    @PostMapping("/api/mail/send")
    public ResponseEntity<Map<String, Object>> sendMailNotification(@RequestBody Map<String, Object> emailData) {
        logger.info("📧 Mail send endpoint accessed");
        return sendEmail(emailData);
    }
}