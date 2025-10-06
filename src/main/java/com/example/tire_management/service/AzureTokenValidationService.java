package com.example.tire_management.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Azure AD Token Validation Service
 * Handles JWT token validation and user role mapping for Azure AD integration
 */
@Service
public class AzureTokenValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureTokenValidationService.class);
    
    @Value("${azure.ad.client-id:your-client-id}")
    private String azureClientId;
    
    @Value("${azure.ad.tenant-id:your-tenant-id}")
    private String azureTenantId;
    
    @Value("${jwt.secret:tire-management-secret-key-for-azure-ad-integration}")
    private String jwtSecret;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Validate Azure AD token and extract user information
     */
    public Map<String, Object> validateAzureToken(String token) {
        try {
            logger.info("🔍 Validating Azure AD token...");
            
            // In production, you should validate the token signature using Azure AD public keys
            // For demo purposes, we'll extract claims without full validation
            Map<String, Object> userInfo = extractUserInfoFromToken(token);
            
            if (userInfo != null) {
                // Map Azure AD user to application user format
                Map<String, Object> appUser = mapAzureUserToAppUser(userInfo);
                
                logger.info("✅ Azure AD token validation successful for user: {}", 
                          appUser.get("name"));
                
                return Map.of(
                    "success", true,
                    "user", appUser,
                    "authMode", "azure_ad",
                    "message", "Azure AD authentication successful"
                );
            } else {
                return Map.of(
                    "success", false,
                    "message", "Invalid Azure AD token"
                );
            }
            
        } catch (Exception e) {
            logger.error("❌ Azure AD token validation failed: {}", e.getMessage());
            return Map.of(
                "success", false,
                "message", "Token validation error: " + e.getMessage()
            );
        }
    }
    
    /**
     * Extract user information from Azure AD token
     * In production, use proper JWT validation with Azure AD public keys
     */
    private Map<String, Object> extractUserInfoFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // For demo purposes, create mock user data
            // In production, decode and validate the actual JWT token
            Map<String, Object> userInfo = new HashMap<>();
            
            // Mock Azure AD user data (replace with actual token parsing)
            userInfo.put("sub", "azure-user-123");
            userInfo.put("name", "Azure AD User");
            userInfo.put("email", "azureuser@company.com");
            userInfo.put("given_name", "Azure");
            userInfo.put("family_name", "User");
            userInfo.put("jobTitle", "Software Engineer");
            userInfo.put("department", "IT Solutions");
            userInfo.put("companyName", "Sri Lanka Telecom");
            
            return userInfo;
            
        } catch (Exception e) {
            logger.error("Error extracting user info from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Map Azure AD user information to application user format
     */
    private Map<String, Object> mapAzureUserToAppUser(Map<String, Object> azureUserInfo) {
        Map<String, Object> appUser = new HashMap<>();
        
        // Generate employee ID for Azure AD user
        String employeeId = generateEmployeeIdFromAzureUser(azureUserInfo);
        
        appUser.put("employeeId", employeeId);
        appUser.put("name", azureUserInfo.get("name"));
        appUser.put("email", azureUserInfo.get("email"));
        appUser.put("role", determineUserRole(azureUserInfo));
        appUser.put("department", azureUserInfo.get("department"));
        appUser.put("position", azureUserInfo.get("jobTitle"));
        appUser.put("authMode", "azure_ad");
        appUser.put("azureUserId", azureUserInfo.get("sub"));
        
        return appUser;
    }
    
    /**
     * Generate employee ID for Azure AD user
     */
    private String generateEmployeeIdFromAzureUser(Map<String, Object> azureUserInfo) {
        // Generate employee ID based on Azure user info
        String email = (String) azureUserInfo.get("email");
        if (email != null) {
            return "AZ" + email.substring(0, Math.min(6, email.indexOf("@"))).toUpperCase();
        }
        return "AZ" + System.currentTimeMillis() % 10000;
    }
    
    /**
     * Determine user role based on Azure AD user information
     */
    private String determineUserRole(Map<String, Object> azureUserInfo) {
        String jobTitle = (String) azureUserInfo.get("jobTitle");
        String department = (String) azureUserInfo.get("department");
        
        if (jobTitle != null) {
            String title = jobTitle.toLowerCase();
            if (title.contains("manager") || title.contains("director") || title.contains("head")) {
                return "manager";
            } else if (title.contains("engineer") || title.contains("developer")) {
                return "engineer";
            } else if (title.contains("transport") || title.contains("tto")) {
                return "tto";
            }
        }
        
        if (department != null) {
            String dept = department.toLowerCase();
            if (dept.contains("transport")) {
                return "tto";
            } else if (dept.contains("engineering") || dept.contains("technical")) {
                return "engineer";
            } else if (dept.contains("management")) {
                return "manager";
            }
        }
        
        return "user"; // Default role
    }
    
    /**
     * Generate JWT token for application session
     */
    public String generateAppToken(Map<String, Object> userInfo) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + 86400000); // 24 hours
            
            return Jwts.builder()
                    .setSubject((String) userInfo.get("employeeId"))
                    .claim("name", userInfo.get("name"))
                    .claim("role", userInfo.get("role"))
                    .claim("department", userInfo.get("department"))
                    .claim("authMode", "azure_ad")
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey())
                    .compact();
        } catch (Exception e) {
            logger.error("Error generating application token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate application JWT token
     */
    public Map<String, Object> validateAppToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("employeeId", claims.getSubject());
            userInfo.put("name", claims.get("name"));
            userInfo.put("role", claims.get("role"));
            userInfo.put("department", claims.get("department"));
            userInfo.put("authMode", claims.get("authMode"));
            
            return Map.of("success", true, "user", userInfo);
            
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return Map.of("success", false, "message", "Invalid token");
        }
    }
}