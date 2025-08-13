package com.example.tire_management.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:3001",
    "https://tire-frontend.vercel.app",
    "https://tire-frontend-git-main-senalridmila2-6843s-projects.vercel.app"
})
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
        List<TireRequest> requests = tireRequestService.getRequestsByStatuses(List.of("PENDING", "MANAGER_APPROVED", "APPROVED"));
        
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
                List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED")
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
            request.setStatus("PENDING");

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
}
