package com.example.tire_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.example.tire_management.model.TireRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Gmail API Direct Integration Service
 * Alternative to SendGrid - Uses Gmail API directly
 * No SMTP restrictions, works on all cloud platforms
 */
@Service
public class GmailAPIService {

    private static final Logger logger = LoggerFactory.getLogger(GmailAPIService.class);

    @Value("${gmail.api.client-id:}")
    private String clientId;

    @Value("${gmail.api.client-secret:}")
    private String clientSecret;

    @Value("${gmail.api.refresh-token:}")
    private String refreshToken;

    @Value("${manager.email:slthrmanager@gmail.com}")
    private String managerEmail;

    @Value("${app.frontend.url:https://tire-slt.vercel.app}")
    private String frontendUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * Send manager notification using Gmail API
     * This is a complete SendGrid replacement
     */
    public boolean sendManagerNotification(TireRequest request) {
        try {
            logger.info("📧 Sending Gmail API notification for request: {}", request.getId());

            // Step 1: Get access token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                logger.error("❌ Failed to get Gmail API access token");
                return false;
            }

            // Step 2: Create email content
            String emailContent = createEmailMessage(request);
            String encodedEmail = Base64.getUrlEncoder().encodeToString(emailContent.getBytes());

            // Step 3: Send via Gmail API
            String apiUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
            
            String requestBody = String.format("""
                {
                    "raw": "%s"
                }
                """, encodedEmail);

            HttpRequest apiRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(apiRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("✅ Gmail API email sent successfully to: {}", managerEmail);
                return true;
            } else {
                logger.error("❌ Gmail API failed. Status: {}, Body: {}", response.statusCode(), response.body());
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Gmail API service error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get Gmail API access token using refresh token
     */
    private String getAccessToken() throws IOException, InterruptedException {
        if (refreshToken == null || refreshToken.isEmpty()) {
            logger.warn("⚠️ Gmail API refresh token not configured");
            return null;
        }

        String tokenUrl = "https://oauth2.googleapis.com/token";
        String requestBody = String.format(
            "client_id=%s&client_secret=%s&refresh_token=%s&grant_type=refresh_token",
            URLEncoder.encode(clientId, StandardCharsets.UTF_8),
            URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
            URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Extract access token from JSON response
            String responseBody = response.body();
            // Simple JSON parsing for access_token
            int start = responseBody.indexOf("\"access_token\":\"") + 16;
            int end = responseBody.indexOf("\"", start);
            return responseBody.substring(start, end);
        }

        logger.error("❌ Failed to get access token. Status: {}", response.statusCode());
        return null;
    }

    /**
     * Create formatted email message for Gmail API
     */
    private String createEmailMessage(TireRequest request) {
        String subject = "🚗 New Tire Request - " + request.getVehicleNo();
        
        String htmlBody = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center;">
                    <h1>🚗 New Tire Request</h1>
                    <p style="font-size: 18px;">Vehicle: <strong>%s</strong></p>
                </div>
                
                <div style="padding: 30px; background: #f8f9fa;">
                    <h2 style="color: #333;">📋 Request Details</h2>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr><td style="padding: 8px; font-weight: bold;">🚗 Vehicle Number:</td><td style="padding: 8px;">%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">🏢 Department:</td><td style="padding: 8px;">%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">🛞 Tire Size:</td><td style="padding: 8px;">%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">📅 Request Date:</td><td style="padding: 8px;">%s</td></tr>
                        <tr><td style="padding: 8px; font-weight: bold;">🆔 Request ID:</td><td style="padding: 8px;">%s</td></tr>
                    </table>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s/manager" style="background: #28a745; color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: bold;">
                            🎯 View Dashboard
                        </a>
                    </div>
                    
                    <div style="background: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="color: #1976d2;">🎯 Next Steps:</h3>
                        <ul style="color: #333;">
                            <li>Review tire request details</li>
                            <li>Check vehicle condition and history</li>
                            <li>Approve or reject the request</li>
                            <li>Notify the requesting department</li>
                        </ul>
                    </div>
                </div>
                
                <div style="background: #333; color: white; padding: 20px; text-align: center;">
                    <p>📧 Sent via Gmail API | 🚗 Tire Management System | SLT</p>
                    <p style="font-size: 12px; color: #ccc;">This is an automated notification from the tire management system.</p>
                </div>
            </body>
            </html>
            """,
            request.getVehicleNo(),
            request.getVehicleNo() != null ? request.getVehicleNo() : "N/A",
            request.getUserSection() != null ? request.getUserSection() : "N/A",
            request.getTireSize() != null ? request.getTireSize() : "N/A",
            java.time.LocalDateTime.now().toString(),
            request.getId(),
            frontendUrl
        );

        // Create raw email message
        return String.format("""
            To: %s
            Subject: %s
            Content-Type: text/html; charset=utf-8
            
            %s
            """, managerEmail, subject, htmlBody);
    }

    /**
     * Test Gmail API connection
     */
    public boolean testConnection() {
        try {
            String accessToken = getAccessToken();
            if (accessToken != null) {
                logger.info("✅ Gmail API connection successful");
                return true;
            } else {
                logger.warn("⚠️ Gmail API connection failed - check configuration");
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Gmail API test failed: {}", e.getMessage());
            return false;
        }
    }
}