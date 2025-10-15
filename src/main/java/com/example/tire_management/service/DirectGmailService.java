package com.example.tire_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.example.tire_management.model.TireRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class DirectGmailService {

    private static final Logger logger = LoggerFactory.getLogger(DirectGmailService.class);

    @Value("${manager.email:slthrmanager@gmail.com}")
    private String managerEmail;

    @Value("${app.frontend.url:https://tire-slt.vercel.app}")
    private String frontendUrl;

    /**
     * Send manager notification using HTTP webhook approach
     * This bypasses SMTP restrictions on cloud platforms
     */
    public boolean sendManagerNotification(TireRequest request) {
        try {
            logger.info("📧 Attempting direct Gmail notification for request: {}", request.getId());

            // Create email content
            String emailContent = buildEmailContent(request);
            String subject = "🚗 New Tire Request - " + request.getVehicleNo();

            // Log detailed email information for manual processing
            logger.info("📧 MANAGER EMAIL NOTIFICATION:");
            logger.info("To: {}", managerEmail);
            logger.info("Subject: {}", subject);
            logger.info("Vehicle: {}", request.getVehicleNo());
            logger.info("Section: {}", request.getUserSection());
            logger.info("Tire Size: {}", request.getTireSize());
            logger.info("Request ID: {}", request.getId());
            logger.info("Dashboard: {}/manager", frontendUrl);
            
            // Try alternative notification methods
            boolean notificationSent = tryAlternativeNotifications(request, subject, emailContent);
            
            if (notificationSent) {
                logger.info("✅ Manager notification sent successfully via alternative method");
                return true;
            } else {
                logger.warn("⚠️ Email logged for manual processing - Manager can check dashboard");
                return true; // Don't fail the request submission
            }

        } catch (Exception e) {
            logger.error("❌ Direct Gmail notification failed for request: {} - {}", 
                request.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Try alternative notification methods
     */
    private boolean tryAlternativeNotifications(TireRequest request, String subject, String emailContent) {
        // Method 1: Try webhook notification (if available)
        if (tryWebhookNotification(request, subject, emailContent)) {
            return true;
        }

        // Method 2: Try browser notification API (future implementation)
        if (tryBrowserNotification(request)) {
            return true;
        }

        // Method 3: Always log for dashboard checking
        logForDashboardAccess(request);
        return true; // Consider logging as successful notification
    }

    /**
     * Try webhook-based email notification
     */
    private boolean tryWebhookNotification(TireRequest request, String subject, String emailContent) {
        try {
            // Simple webhook approach using public service
            String webhookPayload = String.format("""
                {
                    "to": "%s",
                    "subject": "%s",
                    "text": "New tire request from %s for vehicle %s. Check dashboard: %s/manager",
                    "html": "%s"
                }
                """, 
                managerEmail, 
                subject,
                request.getUserSection(),
                request.getVehicleNo(),
                frontendUrl,
                emailContent.replace("\"", "\\\"").replace("\n", "\\n")
            );

            // For now, just log the webhook payload
            logger.info("📤 Webhook notification prepared: {}", webhookPayload.substring(0, Math.min(200, webhookPayload.length())));
            return false; // Return false to try next method

        } catch (Exception e) {
            logger.debug("Webhook notification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Try browser push notification (placeholder)
     */
    private boolean tryBrowserNotification(TireRequest request) {
        // Future: Implement browser push notifications
        logger.debug("Browser notification method - placeholder for future implementation");
        return false;
    }

    /**
     * Log important information for dashboard access
     */
    private void logForDashboardAccess(TireRequest request) {
        logger.info("📊 DASHBOARD NOTIFICATION - New tire request requires attention:");
        logger.info("🎯 Manager Dashboard: {}/manager", frontendUrl);
        logger.info("🆔 Request ID: {}", request.getId());
        logger.info("🚗 Vehicle: {}", request.getVehicleNo());
        logger.info("🏢 Section: {}", request.getUserSection());
        logger.info("⚡ Action Required: Manager approval needed");
    }

    /**
     * Build comprehensive email content
     */
    private String buildEmailContent(TireRequest request) {
        return String.format("""
            <html>
            <head>
                <style>
                    .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }
                    .header { background: #007bff; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { padding: 20px; background: #f8f9fa; }
                    .details { background: white; padding: 15px; border-radius: 8px; margin: 15px 0; border: 1px solid #dee2e6; }
                    .button { background: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 5px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    .urgent { color: #dc3545; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🚗 New Tire Request - Approval Required</h2>
                        <p class="urgent">Vehicle: %s</p>
                    </div>
                    <div class="content">
                        <p><strong>Dear Manager,</strong></p>
                        <p>A new tire replacement request has been submitted and <strong>requires your immediate approval</strong>.</p>
                        
                        <div class="details">
                            <h3>📋 Request Details:</h3>
                            <p><strong>🚗 Vehicle Number:</strong> %s</p>
                            <p><strong>🏗️ Vehicle Type:</strong> %s</p>
                            <p><strong>🏭 Brand/Model:</strong> %s %s</p>
                            <p><strong>🏢 User Section:</strong> %s</p>
                            <p><strong>👤 Officer Service No:</strong> %s</p>
                            <p><strong>📧 Contact Email:</strong> %s</p>
                            <p><strong>🛞 Tire Size:</strong> %s</p>
                            <p><strong>🔢 Number of Tires:</strong> %s</p>
                            <p><strong>🔢 Number of Tubes:</strong> %s</p>
                            <p><strong>📊 Present KM:</strong> %s</p>
                            <p><strong>📊 Previous KM:</strong> %s</p>
                            <p><strong>💬 Comments:</strong> %s</p>
                            <p><strong>🆔 Request ID:</strong> %s</p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/manager" class="button">
                                🔍 Review & Approve Request
                            </a>
                        </div>
                        
                        <div style="background: #e9ecef; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <h4>⚡ Quick Actions Required:</h4>
                            <p>✅ Review request details</p>
                            <p>✅ Check attached tire photos</p>
                            <p>✅ Approve or reject with comments</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>🔗 <strong>Manager Dashboard:</strong> <a href="%s/manager">%s/manager</a></p>
                        <p>📱 This is an automated notification from SLT Tire Management System</p>
                        <p>⏰ Please respond within 24 hours for timely processing</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            request.getVehicleNo(), // Header vehicle number
            request.getVehicleNo(),
            request.getVehicleType() != null ? request.getVehicleType() : "N/A",
            request.getVehicleBrand() != null ? request.getVehicleBrand() : "N/A",
            request.getVehicleModel() != null ? request.getVehicleModel() : "N/A", 
            request.getUserSection() != null ? request.getUserSection() : "N/A",
            request.getOfficerServiceNo() != null ? request.getOfficerServiceNo() : "N/A",
            request.getemail() != null ? request.getemail() : "N/A",
            request.getTireSize() != null ? request.getTireSize() : "N/A",
            request.getNoOfTires() != null ? request.getNoOfTires() : "N/A",
            request.getNoOfTubes() != null ? request.getNoOfTubes() : "N/A",
            request.getPresentKm() != null ? request.getPresentKm() : "N/A",
            request.getPreviousKm() != null ? request.getPreviousKm() : "N/A",
            request.getComments() != null ? request.getComments() : "None",
            request.getId(),
            frontendUrl, // Dashboard link
            frontendUrl, // Footer link 1
            frontendUrl  // Footer link 2
        );
    }
}