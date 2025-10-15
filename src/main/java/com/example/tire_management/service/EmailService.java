package com.example.tire_management.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.tire_management.model.TireRequest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private PDFGeneratorService pdfGeneratorService;

    public JavaMailSender getMailSender() {
        return this.mailSender;
    }

    public PDFGeneratorService getPdfGeneratorService() {
        return this.pdfGeneratorService;
    }



    public void sendOrderConfirmedEmail(TireRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getemail());
            helper.setSubject("Your Tire Order is Confirmed! Order ID: " + request.getId());

            Context context = new Context();
            context.setVariable("request", request);

            // Thymeleaf confirm email template
            String htmlContent = templateEngine.process("email/order-confirmed", context);
            helper.setText(htmlContent, true);

            // Attach PDF if needed
            byte[] pdfBytes = pdfGeneratorService.generateTireRequestPDF(request);
            helper.addAttachment("Tire_Request_" + request.getId() + ".pdf",
                    new ByteArrayResource(pdfBytes), "application/pdf");

            mailSender.send(message);

            logger.info("Confirmation email sent to {}", request.getemail());
        } catch (Exception e) {
            logger.error("Failed to send confirmation email", e);
        }
    }

    // Reject Email Send method
    public void sendOrderRejectedEmail(TireRequest request, String rejectReason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getemail());
            helper.setSubject("Your Tire Order is Rejected - Order ID: " + request.getId());

            Context context = new Context();
            context.setVariable("request", request);
            context.setVariable("rejectReason", rejectReason);

            // Thymeleaf reject email template
            String htmlContent = templateEngine.process("email/order-rejected", context);
            helper.setText(htmlContent, true);

            // Attach PDF if you want, or skip if not required
            byte[] pdfBytes = pdfGeneratorService.generateTireRequestPDF(request);
            helper.addAttachment("Tire_Request_" + request.getId() + ".pdf",
                    new ByteArrayResource(pdfBytes), "application/pdf");

            mailSender.send(message);

            logger.info("Rejection email sent to {}", request.getemail());
        } catch (Exception e) {
            logger.error("Failed to send rejection email", e);
        }
    }

    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;

        this.templateEngine = new TemplateEngine(); // Initialize template engine if needed
    }

    public void sendOrderLinkToUser(TireRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getemail());
            helper.setSubject("Your Tire Request is Approved - Order Now");

            Context context = new Context();
            context.setVariable("requestId", request.getId());
            context.setVariable("orderLink", "https://tire-frontend.vercel.app/order-tires/" + request.getId());

            String htmlContent = templateEngine.process("email/order-link-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send order email to user", e);
        }
    }

    public void sendRequestNotification(TireRequest request, String managerEmail) {
        try {
            // Create the email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setTo(managerEmail);
            helper.setSubject("New Tire Request #" + request.getId());

            // Create the model for template
            Context context = new Context();
            context.setVariable("requestId", request.getId());
            context.setVariable("request", request);
            context.setVariable("reviewUrl", "https://tire-frontend.vercel.app/manager?requestId=" + request.getId());

            // Process the template
            String emailContent = templateEngine.process("email/request-notification", context);
            helper.setText(emailContent, true);

            // Send the email
            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send request notification email", e);
            // You might want to handle this exception more gracefully
        }
    }

    public void sendApprovalNotificationToTTO(TireRequest request, String ttoEmail) {
        logger.info("🚀 STARTING TTO email send process for request ID: {}", request.getId());
        logger.info("📧 TTO Email address: {}", ttoEmail);
        
        try {
            // Create the email message
            logger.debug("Creating MimeMessage...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setTo(ttoEmail);
            String subject = "Tire Request Approved - Action Required #" + request.getId();
            helper.setSubject(subject);
            logger.info("📬 Email TO: {} | SUBJECT: {}", ttoEmail, subject);

            // Create the model for template
            Context context = new Context();
            context.setVariable("requestId", request.getId());
            context.setVariable("request", request);
            context.setVariable("ttoDashboardUrl", "https://tire-frontend.vercel.app/tto/view-request?id=" + request.getId());
            logger.debug("Template context prepared for request: {}", request.getId());

            // Process the template
            logger.debug("Processing email template: email/tto-approval-notification");
            String emailContent = templateEngine.process("email/tto-approval-notification", context);
            helper.setText(emailContent, true);
            
            // Send the email
            logger.info("📤 SENDING EMAIL TO TTO...");
            mailSender.send(message);
            logger.info("✅ SUCCESS: TTO email sent successfully for request {}", request.getId());
            
        } catch (MessagingException e) {
            logger.error("❌ FAILED: MessagingException while sending TTO email for request {}: {}", 
                        request.getId(), e.getMessage(), e);
        } catch (Exception e) {
            logger.error("❌ FAILED: Unexpected error while sending TTO email for request {}: {}", 
                        request.getId(), e.getMessage(), e);
        }
    }

    public void sendEngineerNotification(TireRequest request, String engineerEmail) {
        try {
            // Extensive validation
            if (request == null) {
                logger.error("Cannot send email: Tire request is null");
                return;
            }

            if (engineerEmail == null || engineerEmail.trim().isEmpty()) {
                logger.error("Cannot send email: Engineer email is null or empty");
                return;
            }

            // Log detailed request and email information
            logger.info("Preparing to send engineer notification");
            logger.info("Request ID: {}", request.getId());
            logger.info("Vehicle Number: {}", request.getVehicleNo());
            logger.info("Engineer Email: {}", engineerEmail);

            // Create the email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties with more detailed information
            helper.setFrom("slthrmanager@gmail.com", "SLT Tire Management System");
            helper.setTo(engineerEmail);
            helper.setSubject("🚗 Urgent: Tire Replacement Request #" + request.getId());

            // Create the model for template
            Context context = new Context();
            context.setVariable("request", request);
            context.setVariable("approvalDate", new Date().toString());
            context.setVariable("engineerDashboardUrl", "https://tire-frontend.vercel.app/engineer");
            context.setVariable("requestReviewUrl", "https://tire-frontend.vercel.app/engineer?requestId=" + request.getId());

            // Process the template
            String emailContent = templateEngine.process("email/engineer-notification", context);
            helper.setText(emailContent, true);

            // Generate PDF
            byte[] pdfBytes = pdfGeneratorService.generateTireRequestPDF(request);
            
            // Attach PDF
            helper.addAttachment("Tire_Request_" + request.getId() + ".pdf", 
                new ByteArrayResource(pdfBytes), 
                "application/pdf");

            // Send the email with retry mechanism
            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    mailSender.send(message);
                    logger.info("Engineer notification email sent successfully (Attempt {})", attempt);
                    break; // Exit loop if successful
                } catch (Exception sendException) {
                    logger.error("Email sending failed (Attempt {} of {}): {}", 
                        attempt, maxRetries, sendException.getMessage());
                    
                    // Wait before retrying
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(2000); // Wait 2 seconds before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        // Log final failure
                        logger.error("Failed to send engineer notification after {} attempts", maxRetries);
                        throw sendException;
                    }
                }
            }

        } catch (Exception e) {
            // Comprehensive error logging
            logger.error("Unexpected error in engineer notification process:", e);
            logger.error("Error Details - Request ID: {}, Vehicle No: {}, Engineer Email: {}", 
                request != null ? request.getId() : "N/A", 
                request != null ? request.getVehicleNo() : "N/A",
                engineerEmail);
        }
    }

    
    // 📧 1. Send email to HR Manager when new request is submitted
    public void sendNewRequestNotificationToHR(String userEmail, String vehicleNo, String userSection, String requestId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo("slthrmanager@gmail.com");
            helper.setSubject("🚗 New Tire Request Submitted - " + vehicleNo);
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .header { background: linear-gradient(135deg, #007bff, #0056b3); color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 10px 0; }
                        .button { display: inline-block; background: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                        .footer { background: #6c757d; color: white; padding: 15px; text-align: center; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h2>🚗 New Tire Request Notification</h2>
                        <p>Sri Lanka Telecom - HR Management System</p>
                    </div>
                    <div class="content">
                        <h3>📋 Request Details:</h3>
                        <div class="details">
                            <p><strong>Request ID:</strong> #%s</p>
                            <p><strong>Vehicle Number:</strong> %s</p>
                            <p><strong>User Section:</strong> %s</p>
                            <p><strong>User Email:</strong> %s</p>
                        </div>
                        
                        <a href="https://tire-frontend-main.vercel.app/hr-dashboard" class="button">
                            🏢 Access HR Manager Dashboard
                        </a>
                    </div>
                </body>
                </html>
            """, requestId, vehicleNo, userSection, userEmail);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ HR notification email sent successfully for request: {}", requestId);
            
        } catch (MessagingException e) {
            logger.error("❌ Failed to send HR notification email for request: {}", requestId, e);
        }
    }
    
    // 📧 2. Send email to TTO Officer when HR approves
    public void sendHRApprovalNotificationToTTO(String vehicleNo, String userSection, String requestId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo("slttransportofficer@gmail.com");
            helper.setSubject("✅ HR Approved - Tire Request " + vehicleNo);
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .header { background: linear-gradient(135deg, #28a745, #1e7e34); color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 10px 0; }
                        .button { display: inline-block; background: #17a2b8; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h2>✅ HR Approved Tire Request</h2>
                        <p>Sri Lanka Telecom - TTO Management System</p>
                    </div>
                    <div class="content">
                        <h3>📋 Approved Request Details:</h3>
                        <div class="details">
                            <p><strong>Request ID:</strong> #%s</p>
                            <p><strong>Vehicle Number:</strong> %s</p>
                            <p><strong>User Section:</strong> %s</p>
                            <p><strong>Status:</strong> ✅ HR Approved</p>
                        </div>
                        
                        <a href="https://tire-frontend-main.vercel.app/tto-dashboard" class="button">
                            🔧 Access TTO Dashboard
                        </a>
                    </div>
                </body>
                </html>
            """, requestId, vehicleNo, userSection);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ TTO notification email sent successfully for request: {}", requestId);
            
        } catch (MessagingException e) {
            logger.error("❌ Failed to send TTO notification email for request: {}", requestId, e);
        }
    }
    
    // 📧 3. Send email to Engineer when TTO approves
    public void sendTTOApprovalNotificationToEngineer(String vehicleNo, String userSection, String requestId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo("engineerslt38@gmail.com");
            helper.setSubject("🔧 TTO Approved - Engineering Review Required " + vehicleNo);
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .header { background: linear-gradient(135deg, #17a2b8, #117a8b); color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 10px 0; }
                        .button { display: inline-block; background: #ffc107; color: #212529; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 10px 0; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h2>🔧 Engineering Review Required</h2>
                        <p>Sri Lanka Telecom - Engineering Department</p>
                    </div>
                    <div class="content">
                        <h3>📋 TTO Approved Request:</h3>
                        <div class="details">
                            <p><strong>Request ID:</strong> #%s</p>
                            <p><strong>Vehicle Number:</strong> %s</p>
                            <p><strong>User Section:</strong> %s</p>
                            <p><strong>Status:</strong> ✅ HR + TTO Approved</p>
                        </div>
                        
                        <a href="https://tire-frontend-main.vercel.app/engineer-dashboard" class="button">
                            ⚙️ Access Engineer Dashboard
                        </a>
                    </div>
                </body>
                </html>
            """, requestId, vehicleNo, userSection);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ Engineer notification email sent successfully for request: {}", requestId);
            
        } catch (MessagingException e) {
            logger.error("❌ Failed to send Engineer notification email for request: {}", requestId, e);
        }
    }
    
    // 📧 4. Send confirmation email to User when Engineer approves
    public void sendEngineerApprovalConfirmationToUser(String userEmail, String vehicleNo, String requestId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(userEmail);
            helper.setSubject("🎉 Tire Request Approved - " + vehicleNo);
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .header { background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 10px 0; }
                        .button { display: inline-block; background: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                        .success { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 15px; border-radius: 8px; margin: 10px 0; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h2>🎉 Tire Request Fully Approved!</h2>
                        <p>Sri Lanka Telecom - Request Confirmation</p>
                    </div>
                    <div class="content">
                        <div class="success">
                            <h3>✅ Great News! Your tire request has been approved by all departments.</h3>
                        </div>
                        
                        <div class="details">
                            <p><strong>Request ID:</strong> #%s</p>
                            <p><strong>Vehicle Number:</strong> %s</p>
                            <p><strong>Status:</strong> ✅ HR → TTO → Engineer (All Approved)</p>
                        </div>
                        
                        <a href="https://tire-frontend-main.vercel.app/tire-order" class="button">
                            🛒 Proceed to Tire Order
                        </a>
                    </div>
                </body>
                </html>
            """, requestId, vehicleNo);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ User confirmation email sent successfully for request: {}", requestId);
            
        } catch (MessagingException e) {
            logger.error("❌ Failed to send User confirmation email for request: {}", requestId, e);
        }
    }
    
    // 📧 5. Send tire order notification to Seller
    public void sendTireOrderNotificationToSeller(String vehicleNo, String tireInfo, String quantity, String orderId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo("slttiersellerseller@gmail.com");
            helper.setSubject("🛒 New Tire Order Received - " + vehicleNo);
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .header { background: linear-gradient(135deg, #fd7e14, #e55a4e); color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 10px 0; }
                        .button { display: inline-block; background: #dc3545; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                        .urgent { background: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 15px; border-radius: 8px; margin: 10px 0; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h2>🛒 New Tire Order Alert</h2>
                        <p>Sri Lanka Telecom - Supplier Management System</p>
                    </div>
                    <div class="content">
                        <div class="urgent">
                            <h3>⚡ Urgent: New Tire Order Received</h3>
                        </div>
                        
                        <div class="details">
                            <p><strong>Order ID:</strong> #%s</p>
                            <p><strong>Vehicle Number:</strong> %s</p>
                            <p><strong>Tire Brand/Info:</strong> %s</p>
                            <p><strong>Quantity:</strong> %s tires</p>
                            <p><strong>Status:</strong> 🔄 Pending Seller Action</p>
                        </div>
                        
                        <a href="https://tire-frontend-main.vercel.app/seller-dashboard" class="button">
                            🏪 Access Seller Dashboard
                        </a>
                    </div>
                </body>
                </html>
            """, orderId, vehicleNo, tireInfo, quantity);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ Seller notification email sent successfully for order: {}", orderId);
            
        } catch (MessagingException e) {
            logger.error("❌ Failed to send Seller notification email for order: {}", orderId, e);
        }
    }
    
    /**
     * Generic method to send HTML email - used by frontend email notifications
     * Updated to fix deployment compilation issues
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("slthrmanager@gmail.com"); // Set from address
            
            mailSender.send(message);
            logger.info("✅ HTML email sent successfully to: {}", to);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send HTML email to: {}", to, e);
            throw new MessagingException("Failed to send email: " + e.getMessage());
        }
    }

    // =================================================================================
    // COMPREHENSIVE EMAIL WORKFLOW - Full approval chain
    // =================================================================================

    /**
     * 1. Send email notification to Manager when user submits request
     */
    public void sendManagerNotification(TireRequest request) {
        logger.info("🔄 Attempting to send manager notification email for request: {}", request.getId());
        
        try {
            // Test mail sender connection first
            logger.info("📧 Testing email server connection to smtp.gmail.com...");
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo("slthrmanager@gmail.com");
            helper.setFrom("slthrmanager@gmail.com"); // Explicitly set from address
            helper.setSubject("🚗 New Tire Request Awaiting Approval - " + request.getVehicleNo());
            
            String managerDashboardLink = "https://tire-slt.vercel.app/manager";
            
            String htmlContent = String.format("""
                <html>
                <head>
                    <style>
                        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }
                        .header { background: #007bff; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }
                        .button { background: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 5px; }
                        .button.reject { background: #dc3545; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>🚗 New Tire Request Submitted</h2>
                        </div>
                        <div class="content">
                            <p><strong>Dear Manager,</strong></p>
                            <p>A new tire replacement request has been submitted and requires your approval.</p>
                            
                            <div class="details">
                                <h3>📋 Request Details:</h3>
                                <p><strong>Vehicle Number:</strong> %s</p>
                                <p><strong>Vehicle Type:</strong> %s</p>
                                <p><strong>Brand/Model:</strong> %s %s</p>
                                <p><strong>User Section:</strong> %s</p>
                                <p><strong>Officer Service No:</strong> %s</p>
                                <p><strong>Email:</strong> %s</p>
                                <p><strong>Tire Size:</strong> %s</p>
                                <p><strong>Number of Tires:</strong> %s</p>
                                <p><strong>Number of Tubes:</strong> %s</p>
                                <p><strong>Present KM:</strong> %s</p>
                                <p><strong>Previous KM:</strong> %s</p>
                                <p><strong>Comments:</strong> %s</p>
                            </div>
                            
                            <div style="text-align: center; margin: 25px 0;">
                                <a href="%s" class="button">🔍 Review & Approve in Manager Dashboard</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p>Click the button above to access the Manager Dashboard and approve or reject this request.<br>
                            After your approval, the request will be automatically forwarded to the Transport Officer.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                request.getVehicleNo(), request.getVehicleType(), request.getVehicleBrand(), request.getVehicleModel(),
                request.getUserSection(), request.getOfficerServiceNo(), request.getemail(), 
                request.getTireSize(), request.getNoOfTires(), request.getNoOfTubes(),
                request.getPresentKm(), request.getPreviousKm(), 
                request.getComments() != null ? request.getComments() : "None",
                managerDashboardLink);
            
            helper.setText(htmlContent, true);
            
            logger.info("📤 Sending email to manager: slthrmanager@gmail.com");
            mailSender.send(message);
            
            logger.info("✅ Manager notification email sent successfully for request: {}", request.getId());
            
        } catch (MailException e) {
            if (e.getMessage().contains("connect") || e.getMessage().contains("timeout")) {
                logger.error("🚫 SMTP Connection failed - Gmail server unreachable for request: {}", request.getId());
                logger.error("Connection details: Host=smtp.gmail.com, Port=465 (SSL), Error: {}", e.getMessage());
                throw new RuntimeException("Email service unavailable - Gmail SMTP connection failed", e);
            } else if (e instanceof MailAuthenticationException) {
                logger.error("🔐 Gmail Authentication failed for request: {} - Check app password", request.getId());
                logger.error("Authentication error: {}", e.getMessage());
                throw new RuntimeException("Email authentication failed - Check Gmail credentials", e);
            } else {
                logger.error("📧 Mail service error for request: {}", request.getId(), e);
                throw new RuntimeException("Email service error: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("❌ Unexpected email error for request: {} - {}", request.getId(), e.getClass().getSimpleName());
            logger.error("Full error details: {}", e.getMessage(), e);
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }

    /**
     * 2. Send email notification to TTO when Manager approves
     */
    public void sendTTONotification(TireRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo("slttransportofficer@gmail.com");
            helper.setSubject("🚛 Tire Request Approved by Manager - Awaiting TTO Review - " + request.getVehicleNo());
            
            String ttoDashboardLink = "https://tire-slt.vercel.app/tto";
            
            String htmlContent = String.format("""
                <html>
                <head>
                    <style>
                        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }
                        .header { background: #28a745; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }
                        .button { background: #ffc107; color: #212529; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 5px; font-weight: bold; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>🚛 Manager Approved - TTO Review Required</h2>
                        </div>
                        <div class="content">
                            <p><strong>Dear Transport Officer,</strong></p>
                            <p>A tire request has been <strong>APPROVED</strong> by the Manager and now requires your review and approval.</p>
                            
                            <div class="details">
                                <h3>📋 Approved Request Details:</h3>
                                <p><strong>Vehicle Number:</strong> %s</p>
                                <p><strong>Vehicle Type:</strong> %s</p>
                                <p><strong>User Section:</strong> %s</p>
                                <p><strong>Officer:</strong> %s</p>
                                <p><strong>Tire Requirements:</strong> %s tires, %s tubes</p>
                                <p><strong>Current Status:</strong> Manager Approved ✅</p>
                            </div>
                            
                            <div style="text-align: center; margin: 25px 0;">
                                <a href="%s" class="button">🔍 Review in TTO Dashboard</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p>After your approval, this request will be forwarded to the Engineer for final technical review.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                request.getVehicleNo(), request.getVehicleType(), request.getUserSection(), 
                request.getOfficerServiceNo(), request.getNoOfTires(), request.getNoOfTubes(),
                ttoDashboardLink);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ TTO notification email sent for request: {}", request.getId());
            
        } catch (Exception e) {
            logger.error("❌ Failed to send TTO notification email for request: {}", request.getId(), e);
        }
    }

    /**
     * 3. Send email notification to Engineer when TTO approves
     */
    public void sendEngineerNotification(TireRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo("engineerslt38@gmail.com");
            helper.setSubject("🔧 Tire Request - Final Engineering Approval Required - " + request.getVehicleNo());
            
            String engineerDashboardLink = "https://tire-slt.vercel.app/engineer";
            
            String htmlContent = String.format("""
                <html>
                <head>
                    <style>
                        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }
                        .header { background: #6f42c1; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }
                        .button { background: #17a2b8; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 5px; font-weight: bold; }
                        .status { background: #d4edda; color: #155724; padding: 10px; border-radius: 5px; margin: 10px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>🔧 Final Engineering Approval Required</h2>
                        </div>
                        <div class="content">
                            <p><strong>Dear Engineer,</strong></p>
                            <p>A tire request has been approved by both the <strong>Manager</strong> and <strong>Transport Officer</strong>. Your final engineering approval is now required.</p>
                            
                            <div class="status">
                                <strong>✅ Manager Approved</strong><br>
                                <strong>✅ TTO Approved</strong><br>
                                <strong>⏳ Awaiting Engineering Approval</strong>
                            </div>
                            
                            <div class="details">
                                <h3>🔧 Technical Review Details:</h3>
                                <p><strong>Vehicle:</strong> %s (%s %s)</p>
                                <p><strong>Tire Specifications:</strong> %s</p>
                                <p><strong>Quantity Required:</strong> %s tires, %s tubes</p>
                                <p><strong>Current Mileage:</strong> %s km</p>
                                <p><strong>Previous Mileage:</strong> %s km</p>
                                <p><strong>Requesting Department:</strong> %s</p>
                            </div>
                            
                            <div style="text-align: center; margin: 25px 0;">
                                <a href="%s" class="button">🔧 Final Review in Engineer Dashboard</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p>Your approval will complete the process and notify the requesting user.<br>
                            A tire order will be automatically created for the seller.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                request.getVehicleNo(), request.getVehicleBrand(), request.getVehicleModel(),
                request.getTireSize(), request.getNoOfTires(), request.getNoOfTubes(),
                request.getPresentKm(), request.getPreviousKm(), request.getUserSection(),
                engineerDashboardLink);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ Engineer notification email sent for request: {}", request.getId());
            
        } catch (Exception e) {
            logger.error("❌ Failed to send engineer notification email for request: {}", request.getId(), e);
        }
    }

    /**
     * 4. Send final approval confirmation to User when Engineer approves
     */
    public void sendUserApprovalConfirmation(TireRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(request.getemail());
            helper.setSubject("🎉 Your Tire Request Has Been APPROVED! - " + request.getVehicleNo());
            
            String tireOrderDashboardLink = "https://tire-slt.vercel.app/tire-order";
            
            String htmlContent = String.format("""
                <html>
                <head>
                    <style>
                        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }
                        .header { background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 25px; text-align: center; }
                        .content { padding: 25px; background: #f8f9fa; }
                        .success-badge { background: #d4edda; color: #155724; padding: 15px; border-radius: 8px; text-align: center; margin: 20px 0; border-left: 5px solid #28a745; }
                        .details { background: white; padding: 20px; border-radius: 8px; margin: 15px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .button { background: #fd7e14; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 15px 5px; font-weight: bold; font-size: 16px; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                        .approval-chain { background: #e9ecef; padding: 15px; border-radius: 8px; margin: 15px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 REQUEST APPROVED!</h1>
                            <p style="margin: 0; font-size: 18px;">Your tire replacement request is fully approved</p>
                        </div>
                        <div class="content">
                            <div class="success-badge">
                                <h3 style="margin: 0;">✅ ALL APPROVALS COMPLETED</h3>
                                <p style="margin: 5px 0 0 0;">Manager → Transport Officer → Engineer</p>
                            </div>
                            
                            <p><strong>Dear %s,</strong></p>
                            <p>Great news! Your tire replacement request has been <strong>FULLY APPROVED</strong> by all required authorities.</p>
                            
                            <div class="approval-chain">
                                <h4>📋 Approval Chain:</h4>
                                <p>✅ <strong>Manager</strong> - Approved<br>
                                ✅ <strong>Transport Officer</strong> - Approved<br>
                                ✅ <strong>Engineer</strong> - Final Approval</p>
                            </div>
                            
                            <div class="details">
                                <h3>🚗 Approved Request Summary:</h3>
                                <p><strong>Vehicle Number:</strong> %s</p>
                                <p><strong>Vehicle Type:</strong> %s %s %s</p>
                                <p><strong>Approved Tire Size:</strong> %s</p>
                                <p><strong>Approved Quantity:</strong> %s tires, %s tubes</p>
                                <p><strong>Request ID:</strong> %s</p>
                            </div>
                            
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="%s" class="button">🛞 View Tire Order Dashboard</a>
                            </div>
                            
                            <p><strong>Next Steps:</strong></p>
                            <ul>
                                <li>A tire order has been automatically created</li>
                                <li>The seller has been notified to process your order</li>
                                <li>You can track the progress in the Tire Order Dashboard</li>
                            </ul>
                        </div>
                        <div class="footer">
                            <p>Thank you for using the SL Telecom Tire Management System.<br>
                            For any questions, please contact your department manager.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                request.getOfficerServiceNo(), request.getVehicleNo(),
                request.getVehicleType(), request.getVehicleBrand(), request.getVehicleModel(),
                request.getTireSize(), request.getNoOfTires(), request.getNoOfTubes(),
                request.getId(), tireOrderDashboardLink);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ User approval confirmation email sent to: {} for request: {}", request.getemail(), request.getId());
            
        } catch (Exception e) {
            logger.error("❌ Failed to send user approval confirmation email for request: {}", request.getId(), e);
        }
    }

    /**
     * 5. Send tire order notification to Seller when tire order is created
     */
    public void sendSellerTireOrderNotification(String orderId, String vehicleNo, String tireInfo, String quantity, String userEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo("slttiersellerseller@gmail.com");
            helper.setSubject("🛞 New Tire Order - Processing Required - Order #" + orderId);
            
            String sellerDashboardLink = "https://tire-slt.vercel.app/seller";
            
            String htmlContent = String.format("""
                <html>
                <head>
                    <style>
                        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }
                        .header { background: #e83e8c; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }
                        .button { background: #6f42c1; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 5px; font-weight: bold; }
                        .urgent { background: #fff3cd; color: #856404; padding: 10px; border-radius: 5px; margin: 15px 0; border-left: 5px solid #ffc107; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>🛞 New Tire Order Received</h2>
                        </div>
                        <div class="content">
                            <p><strong>Dear Tire Seller,</strong></p>
                            
                            <div class="urgent">
                                <strong>⚡ URGENT ORDER</strong><br>
                                A new tire order requires immediate processing
                            </div>
                            
                            <p>A fully approved tire request has been converted to an order and requires your immediate attention.</p>
                            
                            <div class="details">
                                <h3>📦 Order Details:</h3>
                                <p><strong>Order ID:</strong> %s</p>
                                <p><strong>Vehicle Number:</strong> %s</p>
                                <p><strong>Tire Specifications:</strong> %s</p>
                                <p><strong>Quantity Required:</strong> %s</p>
                                <p><strong>Customer Email:</strong> %s</p>
                                <p><strong>Order Status:</strong> Pending Processing</p>
                            </div>
                            
                            <div style="text-align: center; margin: 25px 0;">
                                <a href="%s" class="button">🛞 Process Order in Seller Dashboard</a>
                            </div>
                            
                            <p><strong>Required Actions:</strong></p>
                            <ul>
                                <li>Review order specifications</li>
                                <li>Check tire availability</li>
                                <li>Confirm delivery timeline</li>
                                <li>Update order status</li>
                            </ul>
                        </div>
                        <div class="footer">
                            <p>Please process this order promptly to maintain service quality.<br>
                            Contact technical support if you need assistance.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                orderId, vehicleNo, tireInfo, quantity, userEmail, sellerDashboardLink);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            logger.info("✅ Seller tire order notification email sent for order: {}", orderId);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send seller tire order notification email for order: {}", orderId, e);
        }
    }
} 