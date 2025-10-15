package com.example.tire_management.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * 🛠️ Email Testing and Diagnostics Controller
 * For testing email connectivity and troubleshooting SMTP issues
 */
@RestController
@RequestMapping("/api/email-test")
@CrossOrigin(origins = {"https://tire-slt.vercel.app", "http://localhost:3000"})
public class EmailTestController {

    private static final Logger logger = LoggerFactory.getLogger(EmailTestController.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Test basic email connectivity
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testEmailConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🔧 Testing email server connection...");
            
            // Try to create a simple message to test connection
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo("slthrmanager@gmail.com");
            helper.setFrom("slthrmanager@gmail.com");
            helper.setSubject("🔧 Email Connection Test");
            helper.setText("This is a test email to verify SMTP connectivity.", false);
            
            // Try to send
            mailSender.send(message);
            
            response.put("success", true);
            response.put("message", "✅ Email connection successful - Test email sent");
            response.put("status", "SMTP connection working");
            logger.info("✅ Email test successful");
            
            return ResponseEntity.ok(response);
            
        } catch (MailException e) {
            if (e.getMessage().contains("connect") || e.getMessage().contains("timeout")) {
                logger.error("🚫 SMTP Connection failed: {}", e.getMessage());
                response.put("success", false);
                response.put("error", "SMTP Connection Failed");
                response.put("details", "Cannot connect to smtp.gmail.com - " + e.getMessage());
                response.put("suggestion", "Check firewall/network restrictions on Render");
                return ResponseEntity.status(500).body(response);
            } else if (e instanceof MailAuthenticationException) {
                logger.error("🔐 Authentication failed: {}", e.getMessage());
                response.put("success", false);
                response.put("error", "Gmail Authentication Failed");
                response.put("details", e.getMessage());
                response.put("suggestion", "Verify Gmail app password: hxgj pxdl yjou zfbp");
                return ResponseEntity.status(401).body(response);
            } else {
                logger.error("📧 Mail service error: {}", e.getMessage());
                response.put("success", false);
                response.put("error", "Mail Service Error");
                response.put("details", e.getMessage());
                return ResponseEntity.status(500).body(response);
            }
            
        } catch (Exception e) {
            logger.error("❌ Email test failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getClass().getSimpleName());
            response.put("details", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get current email configuration details (for debugging)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getEmailConfig() {
        Map<String, Object> config = new HashMap<>();
        
        try {
            config.put("smtp_host", "smtp.gmail.com");
            config.put("smtp_port", "465 (SSL)");
            config.put("username", "slthrmanager@gmail.com");
            config.put("ssl_enabled", "true");
            config.put("auth_enabled", "true");
            config.put("status", "Configuration loaded");
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            config.put("error", "Failed to load email configuration");
            config.put("details", e.getMessage());
            return ResponseEntity.status(500).body(config);
        }
    }

    /**
     * Alternative email sending using different port (587 TLS)
     */
    @PostMapping("/send-test-tls")
    public ResponseEntity<Map<String, Object>> sendTestTLS() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("🔧 Testing TLS email (port 587)...");
            
            // This would require a secondary mail sender configuration
            // For now, just return info about TLS alternative
            response.put("success", false);
            response.put("message", "TLS alternative not implemented yet");
            response.put("info", "Would use port 587 with STARTTLS instead of 465 SSL");
            response.put("current", "Using port 465 SSL");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}