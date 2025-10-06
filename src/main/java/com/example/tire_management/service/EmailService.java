package com.example.tire_management.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
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




    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = send as HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
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
} 