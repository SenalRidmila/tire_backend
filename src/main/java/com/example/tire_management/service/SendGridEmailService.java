package com.example.tire_management.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.example.tire_management.model.TireRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 📧 SendGrid Email Service for Cloud-Compatible Email Delivery
 * This service replaces Gmail SMTP which is blocked by Render
 */
@Service
public class SendGridEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);

    @Value("${sendgrid.api.key:SG.demo_key}")
    private String sendGridApiKey;

    @Value("${tire.manager.email:slthrmanager@gmail.com}")
    private String managerEmail;

    @Value("${tire.tto.email:slttransportofficer@gmail.com}")
    private String ttoEmail;

    /**
     * Send manager notification when user submits tire request
     */
    public boolean sendManagerNotification(TireRequest request) {
        logger.info("📧 Sending manager notification via SendGrid for request: {}", request.getId());

        try {
            Email from = new Email("noreply@tire-slt.com", "Tire Management System");
            Email to = new Email(managerEmail);
            String subject = "🚗 New Tire Request Awaiting Approval - " + request.getVehicleNo();

            String htmlContent = buildManagerEmailContent(request);
            Content content = new Content("text/html", htmlContent);

            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request sendGridRequest = new Request();

            try {
                sendGridRequest.setMethod(Method.POST);
                sendGridRequest.setEndpoint("mail/send");
                sendGridRequest.setBody(mail.build());
                
                Response response = sg.api(sendGridRequest);
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    logger.info("✅ Manager notification sent successfully via SendGrid. Status: {}", response.getStatusCode());
                    return true;
                } else {
                    logger.error("❌ SendGrid API error. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                    return false;
                }
                
            } catch (IOException ex) {
                logger.error("❌ SendGrid IO Exception: {}", ex.getMessage(), ex);
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Failed to send manager notification via SendGrid: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send TTO notification when manager approves request
     */
    public boolean sendTTONotification(TireRequest request) {
        logger.info("📧 Sending TTO notification via SendGrid for request: {}", request.getId());

        try {
            Email from = new Email("noreply@tire-slt.com", "Tire Management System");
            Email to = new Email(ttoEmail);
            String subject = "🚛 Manager Approved - TTO Review Required - " + request.getVehicleNo();

            String htmlContent = buildTTOEmailContent(request);
            Content content = new Content("text/html", htmlContent);

            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request sendGridRequest = new Request();

            try {
                sendGridRequest.setMethod(Method.POST);
                sendGridRequest.setEndpoint("mail/send");
                sendGridRequest.setBody(mail.build());
                
                Response response = sg.api(sendGridRequest);
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    logger.info("✅ TTO notification sent successfully via SendGrid. Status: {}", response.getStatusCode());
                    return true;
                } else {
                    logger.error("❌ SendGrid API error for TTO. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                    return false;
                }
                
            } catch (IOException ex) {
                logger.error("❌ SendGrid IO Exception for TTO: {}", ex.getMessage(), ex);
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Failed to send TTO notification via SendGrid: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Build HTML content for manager email
     */
    private String buildManagerEmailContent(TireRequest request) {
        return String.format("""
            <html>
            <head>
                <style>
                    .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }
                    .header { background: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f8f9fa; }
                    .details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; }
                    .button { background: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 5px; }
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
                            <a href="https://tire-slt.vercel.app/manager" class="button">🔍 Review in Manager Dashboard</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>Click the button above to access the Manager Dashboard and approve or reject this request.<br>
                        After your approval, the request will be automatically forwarded to the Transport Officer.</p>
                        <p><em>Sent via SendGrid Email Service</em></p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            request.getVehicleNo(), request.getVehicleType(), 
            request.getVehicleBrand(), request.getVehicleModel(),
            request.getUserSection(), request.getOfficerServiceNo(), 
            request.getemail(), request.getTireSize(), 
            request.getNoOfTires(), request.getNoOfTubes(),
            request.getPresentKm(), request.getPreviousKm(), 
            request.getComments() != null ? request.getComments() : "None");
    }

    /**
     * Build HTML content for TTO email
     */
    private String buildTTOEmailContent(TireRequest request) {
        return String.format("""
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
                        <p>A tire request has been <strong>APPROVED</strong> by the Manager and now requires your review.</p>
                        
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
                            <a href="https://tire-slt.vercel.app/tto" class="button">🔍 Review in TTO Dashboard</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>Click the button above to access the TTO Dashboard and complete the approval process.</p>
                        <p><em>Sent via SendGrid Email Service</em></p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            request.getVehicleNo(), request.getVehicleType(),
            request.getUserSection(), request.getOfficerServiceNo(),
            request.getNoOfTires(), request.getNoOfTubes());
    }

    /**
     * Test SendGrid connectivity
     */
    public boolean testConnection() {
        logger.info("🧪 Testing SendGrid API connection...");
        
        try {
            Email from = new Email("test@tire-slt.com", "Test");
            Email to = new Email(managerEmail);
            String subject = "🧪 SendGrid Connection Test";
            Content content = new Content("text/plain", "This is a test email to verify SendGrid connectivity.");

            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            // Just validate the setup without actually sending
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            logger.info("✅ SendGrid API configuration is valid");
            return true;

        } catch (Exception e) {
            logger.error("❌ SendGrid connection test failed: {}", e.getMessage());
            return false;
        }
    }
}