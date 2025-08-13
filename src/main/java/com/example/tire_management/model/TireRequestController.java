package com.example.tire_management.controller;

import com.example.tire_management.model.TireRequest;
import com.example.tire_management.service.TireRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/tire-requests")
public class TireRequestController {

    private static final Logger logger = LoggerFactory.getLogger(TireRequestController.class);

    @Autowired
    private TireRequestService tireRequestService;

    // ...existing code...

    @PostMapping
    public ResponseEntity<TireRequest> createTireRequest(
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
            if (photos != null && photos.length > 0) {
                List<String> photoUrls = new ArrayList<>();
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
                request.setPhotoUrls(photoUrls);
                logger.info("Saved {} photos as Base64 data URLs", photoUrls.size());
            }

            TireRequest savedRequest = tireRequestService.saveTireRequest(request);
            logger.info("Tire request created successfully with ID: {}", savedRequest.getId());
            
            return ResponseEntity.ok(savedRequest);
        } catch (Exception e) {
            logger.error("Error creating tire request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ...existing code...
}