package com.tiremanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SimpleAutoEmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleAutoEmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${manager.email:slthrmanager@gmail.com}")
    private String managerEmail;
    
    @Value("${tto.email:slttransportofficer@gmail.com}")
    private String ttoEmail;
    
    @Value("${engineer.email:engineerslt38@gmail.com}")
    private String engineerEmail;
    
    @Value("${frontend.url:https://tire-frontend-main.vercel.app}")
    private String frontendUrl;
    
    @Value("${backend.url:https://tire-backend-58a9.onrender.com}")
    private String backendUrl;

    // Step 1: Send Manager Notification
    public boolean sendManagerNotification(Object request) {
        try {
            String requestId = getRequestId(request);
            String subject = "New Tire Request - Approval Required #" + requestId;
            
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("Dear Manager,\n\n");
            emailBody.append("A new tire request has been submitted and requires your approval.\n\n");
            emailBody.append("Request Details:\n");
            emailBody.append("Request ID: ").append(requestId).append("\n");
            emailBody.append("User: ").append(getRequestField(request, "userEmail", "N/A")).append("\n");
            emailBody.append("Tire Type: ").append(getRequestField(request, "tireType", "N/A")).append("\n");
            emailBody.append("Tire Size: ").append(getRequestField(request, "tireSize", "N/A")).append("\n");
            emailBody.append("Vehicle Model: ").append(getRequestField(request, "vehicleModel", "N/A")).append("\n");
            emailBody.append("Submitted: ").append(getCurrentTime()).append("\n\n");
            emailBody.append("Please review and approve this request:\n");
            emailBody.append("Approve: ").append(backendUrl).append("/api/tire-requests/manager-approve/").append(requestId).append("\n");
            emailBody.append("Dashboard: ").append(frontendUrl).append("/manager-dashboard\n\n");
            emailBody.append("SL Transport - Tire Management System");
            
            sendEmail(managerEmail, subject, emailBody.toString());
            logger.info("✅ Manager notification sent successfully to: " + managerEmail);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Manager email failed: " + e.getMessage());
            return false;
        }
    }
    
    // Step 2: Send TTO Notification  
    public boolean sendTTONotification(Object request) {
        try {
            String requestId = getRequestId(request);
            String subject = "TTO Approval Required - Tire Request #" + requestId;
            
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("Dear Transport Officer,\n\n");
            emailBody.append("✅ MANAGER APPROVED: This tire request has been approved by the manager and now requires TTO approval.\n\n");
            emailBody.append("Request Details:\n");
            emailBody.append("Request ID: ").append(requestId).append("\n");
            emailBody.append("User: ").append(getRequestField(request, "userEmail", "N/A")).append("\n");
            emailBody.append("Tire Type: ").append(getRequestField(request, "tireType", "N/A")).append("\n");
            emailBody.append("Tire Size: ").append(getRequestField(request, "tireSize", "N/A")).append("\n");
            emailBody.append("Vehicle Model: ").append(getRequestField(request, "vehicleModel", "N/A")).append("\n");
            emailBody.append("Manager Approved: ").append(getCurrentTime()).append("\n\n");
            emailBody.append("Please review and approve for engineering:\n");
            emailBody.append("Approve: ").append(backendUrl).append("/api/tire-requests/tto-approve/").append(requestId).append("\n");
            emailBody.append("TTO Dashboard: ").append(frontendUrl).append("/tto-dashboard\n\n");
            emailBody.append("SL Transport - TTO Department");
            
            sendEmail(ttoEmail, subject, emailBody.toString());
            logger.info("✅ TTO notification sent successfully to: " + ttoEmail);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ TTO email failed: " + e.getMessage());
            return false;
        }
    }
    
    // Step 3: Send Engineer Notification
    public boolean sendEngineerNotification(Object request) {
        try {
            String requestId = getRequestId(request);
            String subject = "Engineering Approval Required - Tire Request #" + requestId;
            
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("Dear Engineer,\n\n");
            emailBody.append("✅ MANAGER APPROVED\n");
            emailBody.append("✅ TTO APPROVED\n");
            emailBody.append("🔧 ENGINEERING REVIEW REQUIRED\n\n");
            emailBody.append("Request Details:\n");
            emailBody.append("Request ID: ").append(requestId).append("\n");
            emailBody.append("User: ").append(getRequestField(request, "userEmail", "N/A")).append("\n");
            emailBody.append("Tire Type: ").append(getRequestField(request, "tireType", "N/A")).append("\n");
            emailBody.append("Tire Size: ").append(getRequestField(request, "tireSize", "N/A")).append("\n");
            emailBody.append("Vehicle Model: ").append(getRequestField(request, "vehicleModel", "N/A")).append("\n");
            emailBody.append("TTO Approved: ").append(getCurrentTime()).append("\n\n");
            emailBody.append("Engineering Checklist:\n");
            emailBody.append("- Verify tire specifications for vehicle compatibility\n");
            emailBody.append("- Check technical requirements and safety standards\n");
            emailBody.append("- Approve for final processing\n\n");
            emailBody.append("Please provide final approval:\n");
            emailBody.append("Approve: ").append(backendUrl).append("/api/tire-requests/engineer-approve/").append(requestId).append("\n");
            emailBody.append("Engineer Dashboard: ").append(frontendUrl).append("/engineer-dashboard\n\n");
            emailBody.append("This is the final approval step before user notification.\n\n");
            emailBody.append("SL Transport - Engineering Department");
            
            sendEmail(engineerEmail, subject, emailBody.toString());
            logger.info("✅ Engineer notification sent successfully to: " + engineerEmail);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Engineer email failed: " + e.getMessage());
            return false;
        }
    }
    
    // Step 4: Send User Final Notification
    public boolean sendUserFinalNotification(Object request, String userEmail) {
        try {
            String requestId = getRequestId(request);
            String subject = "🎉 Tire Request Approved - Request #" + requestId;
            
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("Dear ").append(getRequestField(request, "userEmail", "User")).append(",\n\n");
            emailBody.append("🎉 REQUEST FULLY APPROVED!\n");
            emailBody.append("Your tire request has successfully completed all approval stages.\n\n");
            emailBody.append("Request Summary:\n");
            emailBody.append("Request ID: ").append(requestId).append("\n");
            emailBody.append("Tire Type: ").append(getRequestField(request, "tireType", "N/A")).append("\n");
            emailBody.append("Tire Size: ").append(getRequestField(request, "tireSize", "N/A")).append("\n");
            emailBody.append("Vehicle Model: ").append(getRequestField(request, "vehicleModel", "N/A")).append("\n");
            emailBody.append("Final Status: APPROVED\n");
            emailBody.append("Completion Date: ").append(getCurrentTime()).append("\n\n");
            emailBody.append("Approval Steps Completed:\n");
            emailBody.append("✅ Manager Approval\n");
            emailBody.append("✅ TTO Approval\n");
            emailBody.append("✅ Engineering Approval\n");
            emailBody.append("✅ Request Processing Complete\n\n");
            emailBody.append("Next Steps:\n");
            emailBody.append("- Contact the tire department for scheduling\n");
            emailBody.append("- Present this approval confirmation\n");
            emailBody.append("- Coordinate installation timing\n\n");
            emailBody.append("View Status: ").append(frontendUrl).append("/dashboard\n");
            emailBody.append("New Request: ").append(frontendUrl).append("/request\n\n");
            emailBody.append("Thank you for using the SL Transport Tire Management System!\n\n");
            emailBody.append("SL Transport - Tire Management System");
            
            sendEmail(userEmail, subject, emailBody.toString());
            logger.info("✅ User final notification sent successfully to: " + userEmail);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ User final email failed: " + e.getMessage());
            return false;
        }
    }
    
    // Complete Workflow Test
    public boolean testCompleteWorkflow(String testEmail) {
        try {
            // Create test request
            Object testRequest = createTestRequest();
            
            logger.info("🧪 Starting complete email workflow test...");
            
            // Test all 4 steps
            boolean step1 = sendManagerNotification(testRequest);
            Thread.sleep(1000); // Small delay
            
            boolean step2 = sendTTONotification(testRequest);
            Thread.sleep(1000);
            
            boolean step3 = sendEngineerNotification(testRequest);
            Thread.sleep(1000);
            
            boolean step4 = sendUserFinalNotification(testRequest, testEmail);
            
            boolean allSuccess = step1 && step2 && step3 && step4;
            
            if (allSuccess) {
                logger.info("✅ Complete workflow test SUCCESS! All 4 emails sent.");
            } else {
                logger.warn("⚠️ Workflow test completed with some failures.");
            }
            
            return allSuccess;
            
        } catch (Exception e) {
            logger.error("❌ Workflow test failed: " + e.getMessage());
            return false;
        }
    }

    // Send Simple Email
    private void sendEmail(String to, String subject, String body) throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }
    
    // Utility Methods
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private String getRequestId(Object request) {
        try {
            if (request == null) return "TEST_" + System.currentTimeMillis();
            
            // Use reflection to get ID field
            java.lang.reflect.Field idField = request.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(request);
            return idValue != null ? idValue.toString() : "UNKNOWN_ID";
        } catch (Exception e) {
            return "ID_ERROR_" + System.currentTimeMillis();
        }
    }
    
    private String getRequestField(Object request, String fieldName, String defaultValue) {
        try {
            if (request == null) return defaultValue;
            
            java.lang.reflect.Field field = request.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(request);
            return value != null ? value.toString() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private Object createTestRequest() {
        // Create a simple test object with necessary fields
        return new Object() {
            public final String id = "TEST_" + System.currentTimeMillis();
            public final String userEmail = "test-user@example.com";
            public final String tireType = "Premium Radial";
            public final String tireSize = "225/60R17";
            public final String vehicleModel = "Toyota Camry";
            public final String urgency = "High";
            public final int quantity = 4;
        };
    }
}