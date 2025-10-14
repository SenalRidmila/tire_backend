package com.example.tire_management.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tire_management.service.TireRequestService;

@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private TireRequestService tireRequestService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String loginField = loginRequest.get("email"); // Can be email or employeeId
            String password = loginRequest.get("password");
            
            if (loginField == null || password == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Login credentials are required"));
            }
            
            // Authenticate with either email or employeeId
            Map<String, Object> authResult = authenticateEmployee(loginField, password);
            
            if ((Boolean) authResult.get("success")) {
                logger.info("Successful login for: {}", loginField);
                return ResponseEntity.ok(authResult);
            } else {
                logger.warn("Failed login attempt for: {}", loginField);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authResult);
            }
            
        } catch (Exception e) {
            logger.error("Error during login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        // Simple logout endpoint
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestParam String email) {
        try {
            Map<String, Object> profile = getEmployeeProfile(email);
            if (profile != null) {
                return ResponseEntity.ok(profile);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting profile for email {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Helper method to authenticate employee using MongoDB employees collection
     */
    private Map<String, Object> authenticateEmployee(String loginField, String password) {
        try {
            Map<String, Object> employee = null;
            
            // Check if loginField is an email (contains @) or employeeId
            if (loginField.contains("@")) {
                // Login with email
                employee = tireRequestService.findEmployeeByEmailAndPassword(loginField, password);
            } else {
                // Login with employeeId
                employee = tireRequestService.findEmployeeByEmployeeIdAndPassword(loginField, password);
            }
            
            if (employee != null) {
                // Remove password from response for security
                employee.remove("password");
                
                return Map.of(
                    "success", true,
                    "message", "Login successful",
                    "user", employee,
                    "token", generateSimpleToken(loginField) // Simple token generation
                );
            } else {
                return Map.of(
                    "success", false,
                    "message", "Invalid credentials"
                );
            }
        } catch (Exception e) {
            logger.error("Error authenticating employee: {}", e.getMessage());
            return Map.of(
                "success", false,
                "message", "Authentication error"
            );
        }
    }

    /**
     * Helper method to get employee profile
     */
    private Map<String, Object> getEmployeeProfile(String email) {
        try {
            Map<String, Object> employee = tireRequestService.findEmployeeByEmail(email);
            if (employee != null) {
                // Remove password from response
                employee.remove("password");
                return employee;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting employee profile: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Simple token generation method
     */
    private String generateSimpleToken(String loginField) {
        // Simple token generation - in production, use proper JWT
        return java.util.Base64.getEncoder()
                .encodeToString((loginField + ":" + System.currentTimeMillis()).getBytes());
    }
}
