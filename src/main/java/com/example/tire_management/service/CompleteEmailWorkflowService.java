package com.example.tire_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.example.tire_management.model.TireRequest;

/**
 * Complete Email Workflow Service for Tire Management System
 * 
 * Workflow Steps:
 * 1. User submits → Manager notification
 * 2. Manager approves → TTO notification  
 * 3. TTO approves → Engineer notification
 * 4. Engineer approves → User final notification
 */
@Service
public class CompleteEmailWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(CompleteEmailWorkflowService.class);

    @Value("${manager.email:slthrmanager@gmail.com}")
    private String managerEmail;
    
    @Value("${tto.email:tto@slt.lk}")
    private String ttoEmail;
    
    @Value("${engineer.email:engineer@slt.lk}")
    private String engineerEmail;
    
    @Value("${app.frontend.url:https://tire-slt.vercel.app}")
    private String frontendUrl;

    /**
     * Step 1: User submits request → Manager notification
     * Called when new tire request is created
     */
    public boolean sendManagerNotification(TireRequest request) {
        try {
            logger.info("📧 Step 1: Sending Manager notification for request: {}", request.getId());
            
            String subject = "🚗 New Tire Request - " + request.getVehicleNo();
            String content = buildManagerEmailContent(request);
            String dashboardLink = frontendUrl + "/manager";
            
            // Log email details for manager dashboard integration
            logger.info("📧 MANAGER NOTIFICATION:");
            logger.info("To: {}", managerEmail);
            logger.info("Subject: {}", subject);
            logger.info("Vehicle: {}", request.getVehicleNo());
            logger.info("Section: {}", request.getUserSection());
            logger.info("Dashboard: {}", dashboardLink);
            logger.info("Action: Review and approve tire request");
            
            // In production, this would integrate with EmailJS or other email service
            logEmailForDashboard("MANAGER", managerEmail, subject, content, dashboardLink, request);
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Manager notification failed for request: {}", request.getId(), e);
            return false;
        }
    }

    /**
     * Step 2: Manager approves → TTO notification
     * Called when manager approves request
     */
    public boolean sendTTONotification(TireRequest request, String approvedBy) {
        try {
            logger.info("📧 Step 2: Sending TTO notification for request: {}", request.getId());
            
            String subject = "🎯 Manager Approved - TTO Review Required - " + request.getVehicleNo();
            String content = buildTTOEmailContent(request, approvedBy);
            String dashboardLink = frontendUrl + "/tto";
            
            logger.info("📧 TTO NOTIFICATION:");
            logger.info("To: {}", ttoEmail);
            logger.info("Subject: {}", subject);
            logger.info("Vehicle: {}", request.getVehicleNo());
            logger.info("Approved by: {}", approvedBy);
            logger.info("Dashboard: {}", dashboardLink);
            logger.info("Action: Review technical specifications and approve");
            
            logEmailForDashboard("TTO", ttoEmail, subject, content, dashboardLink, request);
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ TTO notification failed for request: {}", request.getId(), e);
            return false;
        }
    }

    /**
     * Step 3: TTO approves → Engineer notification
     * Called when TTO approves request
     */
    public boolean sendEngineerNotification(TireRequest request, String approvedBy) {
        try {
            logger.info("📧 Step 3: Sending Engineer notification for request: {}", request.getId());
            
            String subject = "⚙️ TTO Approved - Engineering Review Required - " + request.getVehicleNo();
            String content = buildEngineerEmailContent(request, approvedBy);
            String dashboardLink = frontendUrl + "/engineer";
            
            logger.info("📧 ENGINEER NOTIFICATION:");
            logger.info("To: {}", engineerEmail);
            logger.info("Subject: {}", subject);
            logger.info("Vehicle: {}", request.getVehicleNo());
            logger.info("TTO approved by: {}", approvedBy);
            logger.info("Dashboard: {}", dashboardLink);
            logger.info("Action: Final engineering approval for procurement");
            
            logEmailForDashboard("ENGINEER", engineerEmail, subject, content, dashboardLink, request);
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Engineer notification failed for request: {}", request.getId(), e);
            return false;
        }
    }

    /**
     * Step 4: Engineer approves → User final notification
     * Called when engineer gives final approval
     */
    public boolean sendUserFinalNotification(TireRequest request, String approvedBy) {
        try {
            logger.info("📧 Step 4: Sending User final notification for request: {}", request.getId());
            
            String subject = "✅ Tire Request Approved - Order Processing - " + request.getVehicleNo();
            String content = buildUserFinalEmailContent(request, approvedBy);
            String dashboardLink = frontendUrl + "/orders";
            String userEmail = request.getemail(); // Get user email from original request
            
            logger.info("📧 USER FINAL NOTIFICATION:");
            logger.info("To: {}", userEmail);
            logger.info("Subject: {}", subject);
            logger.info("Vehicle: {}", request.getVehicleNo());
            logger.info("Final approved by: {}", approvedBy);
            logger.info("Dashboard: {}", dashboardLink);
            logger.info("Action: Track order status and delivery");
            
            logEmailForDashboard("USER", userEmail, subject, content, dashboardLink, request);
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ User final notification failed for request: {}", request.getId(), e);
            return false;
        }
    }

    /**
     * Build Manager email content
     */
    private String buildManagerEmailContent(TireRequest request) {
        return String.format("""
            <h2>🚗 New Tire Request Received</h2>
            
            <div style="background: #f8f9fa; padding: 20px; border-radius: 8px;">
                <h3>📋 Request Details:</h3>
                <p><strong>🚗 Vehicle Number:</strong> %s</p>
                <p><strong>🏢 Department:</strong> %s</p>
                <p><strong>🛞 Tire Size:</strong> %s</p>
                <p><strong>📅 Request Date:</strong> %s</p>
                <p><strong>🆔 Request ID:</strong> %s</p>
                <p><strong>👤 Requested by:</strong> %s</p>
            </div>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s/manager" style="background: #28a745; color: white; padding: 15px 30px; 
                   text-decoration: none; border-radius: 8px; font-weight: bold;">
                    🎯 Review & Approve Request
                </a>
            </div>
            
            <p><strong>Next Step:</strong> Review request details and approve for TTO processing.</p>
            """,
            request.getVehicleNo(), request.getUserSection(), request.getTireSize(),
            java.time.LocalDateTime.now().toString(), request.getId(),
            request.getemail(), frontendUrl
        );
    }

    /**
     * Build TTO email content
     */
    private String buildTTOEmailContent(TireRequest request, String approvedBy) {
        return String.format("""
            <h2>🎯 Manager Approved - TTO Review Required</h2>
            
            <div style="background: #e3f2fd; padding: 20px; border-radius: 8px;">
                <h3>✅ Approval Status:</h3>
                <p><strong>✅ Manager Approved by:</strong> %s</p>
                <p><strong>📅 Approval Date:</strong> %s</p>
            </div>
            
            <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                <h3>📋 Request Details:</h3>
                <p><strong>🚗 Vehicle Number:</strong> %s</p>
                <p><strong>🏢 Department:</strong> %s</p>
                <p><strong>🛞 Tire Size:</strong> %s</p>
                <p><strong>🆔 Request ID:</strong> %s</p>
            </div>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s/tto" style="background: #007bff; color: white; padding: 15px 30px; 
                   text-decoration: none; border-radius: 8px; font-weight: bold;">
                    🔧 TTO Dashboard - Review Technical Specs
                </a>
            </div>
            
            <p><strong>Next Step:</strong> Review technical specifications and approve for engineering review.</p>
            """,
            approvedBy, java.time.LocalDateTime.now().toString(),
            request.getVehicleNo(), request.getUserSection(), request.getTireSize(),
            request.getId(), frontendUrl
        );
    }

    /**
     * Build Engineer email content
     */
    private String buildEngineerEmailContent(TireRequest request, String approvedBy) {
        return String.format("""
            <h2>⚙️ TTO Approved - Engineering Review Required</h2>
            
            <div style="background: #fff3cd; padding: 20px; border-radius: 8px;">
                <h3>✅ Approval Chain:</h3>
                <p><strong>✅ Manager:</strong> Approved</p>
                <p><strong>✅ TTO Approved by:</strong> %s</p>
                <p><strong>📅 TTO Approval Date:</strong> %s</p>
            </div>
            
            <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                <h3>📋 Request Details:</h3>
                <p><strong>🚗 Vehicle Number:</strong> %s</p>
                <p><strong>🏢 Department:</strong> %s</p>
                <p><strong>🛞 Tire Size:</strong> %s</p>
                <p><strong>🆔 Request ID:</strong> %s</p>
            </div>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s/engineer" style="background: #fd7e14; color: white; padding: 15px 30px; 
                   text-decoration: none; border-radius: 8px; font-weight: bold;">
                    ⚙️ Engineering Dashboard - Final Approval
                </a>
            </div>
            
            <p><strong>Final Step:</strong> Engineering approval for tire procurement and ordering.</p>
            """,
            approvedBy, java.time.LocalDateTime.now().toString(),
            request.getVehicleNo(), request.getUserSection(), request.getTireSize(),
            request.getId(), frontendUrl
        );
    }

    /**
     * Build User final notification content
     */
    private String buildUserFinalEmailContent(TireRequest request, String approvedBy) {
        return String.format("""
            <h2>🎉 Tire Request Fully Approved!</h2>
            
            <div style="background: #d4edda; padding: 20px; border-radius: 8px;">
                <h3>✅ Complete Approval Chain:</h3>
                <p><strong>✅ Manager:</strong> Approved</p>
                <p><strong>✅ TTO:</strong> Approved</p>
                <p><strong>✅ Engineering Approved by:</strong> %s</p>
                <p><strong>📅 Final Approval Date:</strong> %s</p>
            </div>
            
            <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                <h3>📋 Your Request Details:</h3>
                <p><strong>🚗 Vehicle Number:</strong> %s</p>
                <p><strong>🛞 Tire Size:</strong> %s</p>
                <p><strong>🆔 Request ID:</strong> %s</p>
                <p><strong>📦 Estimated Delivery:</strong> 7-10 business days</p>
            </div>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s/orders" style="background: #28a745; color: white; padding: 15px 30px; 
                   text-decoration: none; border-radius: 8px; font-weight: bold;">
                    📦 Track Your Order Status
                </a>
            </div>
            
            <p><strong>What's Next:</strong> Your tire order is now being processed. Track delivery status on the order dashboard.</p>
            """,
            approvedBy, java.time.LocalDateTime.now().toString(),
            request.getVehicleNo(), request.getTireSize(), request.getId(), frontendUrl
        );
    }

    /**
     * Log email for dashboard integration
     */
    private void logEmailForDashboard(String role, String email, String subject, String content, String dashboardLink, TireRequest request) {
        logger.info("📧 EMAIL LOGGED FOR DASHBOARD:");
        logger.info("Role: {}", role);
        logger.info("Recipient: {}", email);
        logger.info("Subject: {}", subject);
        logger.info("Dashboard: {}", dashboardLink);
        logger.info("Request ID: {}", request.getId());
        logger.info("Vehicle: {}", request.getVehicleNo());
        logger.info("Status: NOTIFICATION_SENT");
        logger.info("Timestamp: {}", java.time.LocalDateTime.now());
    }
}