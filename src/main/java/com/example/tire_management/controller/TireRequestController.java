package com.example.tire_management.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.tire_management.model.TireRequest;
import com.example.tire_management.service.TireRequestService;

// Add Employee model import (we'll need to create this)
// import com.example.tire_management.model.Employee;
// import com.example.tire_management.service.EmployeeService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tire-requests")
public class TireRequestController {

    private static final Logger logger = LoggerFactory.getLogger(TireRequestController.class);

    @Autowired
    private TireRequestService tireRequestService;

    // ----------------- Common GETs -----------------
    @GetMapping
    public List<TireRequest> getAllTireRequests() {
        List<TireRequest> requests = tireRequestService.getAllTireRequests();
        
        // Ensure photos are properly consolidated for each request
        for (TireRequest request : requests) {
            consolidatePhotos(request);
        }
        
        logger.info("Retrieved {} tire requests with consolidated photos", requests.size());
        return requests;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TireRequest> getTireRequestById(@PathVariable String id) {
        return tireRequestService.getTireRequestById(id)
                .map(request -> {
                    consolidatePhotos(request);
                    return ResponseEntity.ok(request);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    

    // ----------------- Role based GETs -----------------
    // Manager dashboard – show Pending + already approved by manager (if you need)
    @GetMapping("/manager/requests")
    public List<TireRequest> getRequestsForManager() {
        List<TireRequest> requests = tireRequestService.getRequestsByStatuses(List.of("pending", "PENDING", "MANAGER_APPROVED", "APPROVED"));
        
        // Ensure photos are properly consolidated for each request
        for (TireRequest request : requests) {
            consolidatePhotos(request);
        }
        
        logger.info("Retrieved {} manager requests with consolidated photos", requests.size());
        return requests;
    }

    // TTO dashboard – DO NOT hide after TTO action; show all relevant
    @GetMapping("/tto/requests")
    public List<TireRequest> getRequestsForTTO() {
        List<TireRequest> requests = tireRequestService.getRequestsByStatuses(
                List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved", "pending")
        );
        
        // Ensure photos are properly consolidated for each request
        for (TireRequest request : requests) {
            consolidatePhotos(request);
        }
        
        logger.info("Retrieved {} TTO requests with consolidated photos", requests.size());
        return requests;
    }

    // Engineer dashboard – if engineers only see TTO approved ones
    @GetMapping("/engineer/requests")
    public List<TireRequest> getRequestsForEngineer() {
        List<TireRequest> requests = tireRequestService.getRequestsByStatuses(List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED"));
        
        // Ensure photos are properly consolidated for each request
        for (TireRequest request : requests) {
            consolidatePhotos(request);
        }
        
        logger.info("Retrieved {} engineer requests with consolidated photos", requests.size());
        return requests;
    }



    // ----------------- Create / Update / Delete -----------------
    @PostMapping
    public ResponseEntity<TireRequest> createTireRequest(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "tirePhotos", required = false) List<MultipartFile> tirePhotos) {

        try {
            TireRequest request = buildTireRequestFromParams(params);

            List<String> photoUrls = saveUploadedFiles(tirePhotos);
            // Save to both photo fields for consistency
            request.setTirePhotoUrls(photoUrls);
            request.setPhotoUrls(photoUrls);

            TireRequest createdRequest = tireRequestService.createTireRequest(request);
            logger.info("Created tire request with {} photos", photoUrls.size());
            return ResponseEntity.ok(createdRequest);
        } catch (IOException e) {
            logger.error("Error creating tire request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Alternative endpoint for form-based submission (matching frontend)
    @PostMapping("/create")
    public ResponseEntity<TireRequest> createTireRequestForm(
            @RequestParam("vehicleModel") String vehicleModel,
            @RequestParam("section") String section,
            @RequestParam("tireSize") String tireSize,
            @RequestParam("numberOfTires") int numberOfTires,
            @RequestParam("numberOfTubes") int numberOfTubes,
            @RequestParam("presentKm") int presentKm,
            @RequestParam("previousKm") int previousKm,
            @RequestParam("wearPattern") String wearPattern,
            @RequestParam("officerName") String officerName,
            @RequestParam("email") String email,
            @RequestParam(value = "photos", required = false) MultipartFile[] photos) {

        try {
            TireRequest request = new TireRequest();
            request.setVehicleModel(vehicleModel);
            request.setUserSection(section);
            request.setTireSize(tireSize);
            request.setNoOfTires(String.valueOf(numberOfTires));
            request.setNoOfTubes(String.valueOf(numberOfTubes));
            request.setPresentKm(String.valueOf(presentKm));
            request.setPreviousKm(String.valueOf(previousKm));
            request.setWearPattern(wearPattern);
            request.setOfficerServiceNo(officerName);
            request.setemail(email);
            request.setReplacementDate(new Date().toString());
            request.setStatus("pending");

            // Handle photo uploads
            List<String> photoUrls = new ArrayList<>();
            if (photos != null && photos.length > 0) {
                for (MultipartFile photo : photos) {
                    if (!photo.isEmpty()) {
                        try {
                            // Convert to Base64 with proper MIME type
                            String contentType = photo.getContentType();
                            if (contentType == null) {
                                contentType = "image/jpeg"; // default
                            }
                            
                            byte[] photoBytes = photo.getBytes();
                            String base64Image = Base64.getEncoder().encodeToString(photoBytes);
                            String dataUrl = "data:" + contentType + ";base64," + base64Image;
                            
                            photoUrls.add(dataUrl);
                            logger.info("Successfully converted photo to Base64. Size: {} bytes, Type: {}", 
                                       photoBytes.length, contentType);
                        } catch (Exception e) {
                            logger.error("Error processing photo: {}", e.getMessage());
                            // Continue with other photos
                        }
                    }
                }
            }
            
            // Save to both photo fields for consistency
            request.setTirePhotoUrls(photoUrls);
            request.setPhotoUrls(photoUrls);
            logger.info("Saved {} photos for new request", photoUrls.size());

            TireRequest savedRequest = tireRequestService.createTireRequest(request);
            logger.info("Tire request created successfully with ID: {}", savedRequest.getId());
            
            return ResponseEntity.ok(savedRequest);
        } catch (Exception e) {
            logger.error("Error creating tire request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TireRequest> updateTireRequest(
            @PathVariable String id,
            @RequestParam Map<String, String> params,
            @RequestParam(value = "tirePhotos", required = false) List<MultipartFile> tirePhotos) {

        try {
            TireRequest request = buildTireRequestFromParams(params);

            List<String> photoUrls = saveUploadedFiles(tirePhotos);
            // Save to both photo fields for consistency
            request.setTirePhotoUrls(photoUrls);
            request.setPhotoUrls(photoUrls);

            TireRequest updatedRequest = tireRequestService.updateTireRequest(id, request);
            logger.info("Updated tire request {} with {} photos", id, photoUrls.size());
            return ResponseEntity.ok(updatedRequest);
        } catch (IOException e) {
            logger.error("Error updating tire request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getRequestPDF(@PathVariable String id) {
        try {
            byte[] pdfData = tireRequestService.generateRequestPDF(id); // You must implement this method in the service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("inline")
                    .filename("tire_request_" + id + ".pdf").build());

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTireRequest(@PathVariable String id) {
        tireRequestService.deleteTireRequest(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TireRequest> updateTireRequestStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        try {
            TireRequest updatedRequest = tireRequestService.updateTireRequestStatus(id, updates);
            return ResponseEntity.ok(updatedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ----------------- Status-only Update -----------------
    

    // ----------------- Manager actions -----------------
    @PostMapping("/{id}/approve")
    public ResponseEntity<TireRequest> approveTireRequest(@PathVariable String id) {
        try {
            return ResponseEntity.ok(tireRequestService.approveTireRequest(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<TireRequest> rejectTireRequest(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.get("reason");
            return ResponseEntity.ok(tireRequestService.rejectTireRequest(id, reason));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/tto-approve")
    public ResponseEntity<TireRequest> approveTireRequestByTTO(@PathVariable String id) {
        try {
            TireRequest approvedRequest = tireRequestService.approveTireRequestByTTO(id);
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @PostMapping("/{id}/engineer-approve")
    public ResponseEntity<String> engineerApprove(@PathVariable String id) {
        tireRequestService.approveByEngineer(id);
        return ResponseEntity.ok("Request approved and email sent.");
    }

    @PostMapping("/{id}/engineer-reject")
    public ResponseEntity<?> engineerReject(@PathVariable String id) {
        tireRequestService.rejectByEngineer(id);
        return ResponseEntity.ok().build();
    }

    // ----------------- Authentication Endpoints -----------------
    @PostMapping("/auth/login")
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

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        // Simple logout endpoint
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/auth/profile")
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

    private TireRequest buildTireRequestFromParams(Map<String, String> params) {
        TireRequest request = new TireRequest();
        request.setVehicleNo(params.getOrDefault("vehicleNo", ""));
        request.setVehicleType(params.getOrDefault("vehicleType", ""));
        request.setVehicleBrand(params.getOrDefault("vehicleBrand", ""));
        request.setVehicleModel(params.getOrDefault("vehicleModel", ""));
        request.setUserSection(params.getOrDefault("userSection", ""));
        request.setReplacementDate(params.getOrDefault("replacementDate", ""));
        request.setExistingMake(params.getOrDefault("existingMake", ""));
        request.setTireSize(params.getOrDefault("tireSize", ""));
        request.setNoOfTires(params.getOrDefault("noOfTires", ""));
        request.setNoOfTubes(params.getOrDefault("noOfTubes", ""));
        request.setCostCenter(params.getOrDefault("costCenter", ""));
        request.setPresentKm(params.getOrDefault("presentKm", ""));
        request.setPreviousKm(params.getOrDefault("previousKm", ""));
        request.setWearIndicator(params.getOrDefault("wearIndicator", ""));
        request.setWearPattern(params.getOrDefault("wearPattern", ""));
        request.setOfficerServiceNo(params.getOrDefault("officerServiceNo", ""));
        request.setemail(params.getOrDefault("email", ""));
        request.setComments(params.getOrDefault("comments", ""));
        return request;
    }

    private List<String> saveUploadedFiles(List<MultipartFile> files) throws IOException {
        List<String> photoUrls = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        // Validate file type
                        String contentType = file.getContentType();
                        if (contentType == null || (!contentType.startsWith("image/"))) {
                            logger.warn("Invalid file type: {}. Skipping file.", contentType);
                            continue;
                        }
                        
                        // Convert to Base64 for database storage
                        byte[] fileBytes = file.getBytes();
                        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
                        String dataUrl = "data:" + contentType + ";base64," + base64Image;
                        
                        photoUrls.add(dataUrl);
                        logger.info("Successfully processed photo. Size: {} bytes, Type: {}", 
                                   fileBytes.length, contentType);
                    } catch (Exception e) {
                        logger.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage());
                        // Continue with other files instead of failing completely
                    }
                }
            }
        }

        logger.info("Processed {} photos successfully", photoUrls.size());
        return photoUrls;
    }

    @GetMapping("/{id}/photos")
    public ResponseEntity<List<String>> getTireRequestPhotos(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request != null) {
                List<String> photos = new ArrayList<>();
                
                // Check both photo fields to ensure we get all photos
                if (request.getTirePhotoUrls() != null && !request.getTirePhotoUrls().isEmpty()) {
                    photos.addAll(request.getTirePhotoUrls());
                }
                if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
                    // Avoid duplicates
                    for (String photo : request.getPhotoUrls()) {
                        if (!photos.contains(photo)) {
                            photos.add(photo);
                        }
                    }
                }
                
                logger.info("Retrieved {} photos for request {}", photos.size(), id);
                return ResponseEntity.ok(photos);
            } else {
                logger.warn("No tire request found with id: {}", id);
                return ResponseEntity.ok(new ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("Error getting photos for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    // New endpoint specifically for table photo display
    @GetMapping("/{id}/photos/count")
    public ResponseEntity<Map<String, Object>> getPhotoCount(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request != null) {
                consolidatePhotos(request);
                
                int photoCount = 0;
                List<String> photoUrls = new ArrayList<>();
                
                if (request.getPhotoUrls() != null) {
                    photoCount = request.getPhotoUrls().size();
                    photoUrls = request.getPhotoUrls();
                }
                
                Map<String, Object> response = Map.of(
                    "count", photoCount,
                    "hasPhotos", photoCount > 0,
                    "photos", photoUrls
                );
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of("count", 0, "hasPhotos", false, "photos", new ArrayList<>()));
            }
        } catch (Exception e) {
            logger.error("Error getting photo count for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("count", 0, "hasPhotos", false, "photos", new ArrayList<>()));
        }
    }

    // Debug endpoint to check photos
    @GetMapping("/debug/photos")
    public ResponseEntity<Map<String, Object>> debugPhotos() {
        try {
            List<TireRequest> allRequests = tireRequestService.getAllTireRequests();
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("totalRequests", allRequests.size());
            
            List<Map<String, Object>> requestsWithPhotos = new ArrayList<>();
            
            for (TireRequest request : allRequests) {
                Map<String, Object> requestInfo = new HashMap<>();
                requestInfo.put("id", request.getId());
                requestInfo.put("vehicleModel", request.getVehicleModel());
                
                // Count photos from both fields
                int tirePhotoCount = request.getTirePhotoUrls() != null ? request.getTirePhotoUrls().size() : 0;
                int photoCount = request.getPhotoUrls() != null ? request.getPhotoUrls().size() : 0;
                
                requestInfo.put("tirePhotoUrls_count", tirePhotoCount);
                requestInfo.put("photoUrls_count", photoCount);
                requestInfo.put("totalPhotos", Math.max(tirePhotoCount, photoCount));
                
                if (tirePhotoCount > 0 || photoCount > 0) {
                    requestsWithPhotos.add(requestInfo);
                }
            }
            
            debugInfo.put("requestsWithPhotos", requestsWithPhotos);
            debugInfo.put("requestsWithPhotosCount", requestsWithPhotos.size());
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("Error in debug photos endpoint: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Debug endpoint to check status distribution
    @GetMapping("/debug/status")
    public ResponseEntity<Map<String, Object>> debugStatus() {
        try {
            List<TireRequest> allRequests = tireRequestService.getAllTireRequests();
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("totalRequests", allRequests.size());
            
            // Count requests by status
            Map<String, Integer> statusCounts = new HashMap<>();
            List<Map<String, Object>> requestDetails = new ArrayList<>();
            
            for (TireRequest request : allRequests) {
                String status = request.getStatus();
                statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
                
                Map<String, Object> requestInfo = new HashMap<>();
                requestInfo.put("id", request.getId());
                requestInfo.put("status", status);
                requestInfo.put("vehicleModel", request.getVehicleModel());
                requestInfo.put("vehicleNo", request.getVehicleNo());
                requestDetails.add(requestInfo);
            }
            
            debugInfo.put("statusCounts", statusCounts);
            debugInfo.put("requestDetails", requestDetails);
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("Error in debug status endpoint: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Helper method to consolidate photos from both tirePhotoUrls and photoUrls fields
     * This ensures that photos are always available in both fields for frontend compatibility
     */
    private void consolidatePhotos(TireRequest request) {
        if (request == null) return;
        
        List<String> allPhotos = new ArrayList<>();
        
        // Collect photos from both fields
        if (request.getTirePhotoUrls() != null && !request.getTirePhotoUrls().isEmpty()) {
            allPhotos.addAll(request.getTirePhotoUrls());
        }
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            // Only add if not already in the list (avoid duplicates)
            for (String photo : request.getPhotoUrls()) {
                if (!allPhotos.contains(photo)) {
                    allPhotos.add(photo);
                }
            }
        }
        
        // Update both fields with consolidated photos
        request.setTirePhotoUrls(allPhotos);
        request.setPhotoUrls(allPhotos);
        
        if (!allPhotos.isEmpty()) {
            logger.debug("Consolidated {} photos for request {}", allPhotos.size(), request.getId());
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
                    "message", "Invalid email or password"
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
     * Simple token generation (in production, use JWT or similar)
     */
    private String generateSimpleToken(String email) {
        return Base64.getEncoder().encodeToString((email + ":" + System.currentTimeMillis()).getBytes());
    }
}
