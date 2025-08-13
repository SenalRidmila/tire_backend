package com.example.tire_management.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
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
        return tireRequestService.getAllTireRequests();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TireRequest> getTireRequestById(@PathVariable String id) {
        return tireRequestService.getTireRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    

    // ----------------- Role based GETs -----------------
    // Manager dashboard – show Pending + already approved by manager (if you need)
    @GetMapping("/manager/requests")
    public List<TireRequest> getRequestsForManager() {
        return tireRequestService.getRequestsByStatuses(List.of("PENDING", "MANAGER_APPROVED", "APPROVED"));
    }

    // TTO dashboard – DO NOT hide after TTO action; show all relevant
    @GetMapping("/tto/requests")
    public List<TireRequest> getRequestsForTTO() {
        return tireRequestService.getRequestsByStatuses(
                List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED")
        );
    }




    // Engineer dashboard – if engineers only see TTO approved ones
    @GetMapping("/engineer/requests")
    public List<TireRequest> getRequestsForEngineer() {
        // Return all requests with status TTO_APPROVED, ENGINEER_APPROVED, ENGINEER_REJECTED
        return tireRequestService.getRequestsByStatuses(List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED"));
    }



    // ----------------- Create / Update / Delete -----------------
    @PostMapping
    public ResponseEntity<TireRequest> createTireRequest(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "tirePhotos", required = false) List<MultipartFile> tirePhotos) {

        try {
            TireRequest request = buildTireRequestFromParams(params);

            List<String> photoUrls = saveUploadedFiles(tirePhotos);
            request.setTirePhotoUrls(photoUrls);

            TireRequest createdRequest = tireRequestService.createTireRequest(request);
            return ResponseEntity.ok(createdRequest);
        } catch (IOException e) {
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
            request.setTirePhotoUrls(photoUrls);

            TireRequest updatedRequest = tireRequestService.updateTireRequest(id, request);
            return ResponseEntity.ok(updatedRequest);
        } catch (IOException e) {
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
                    // Convert to Base64 for database storage (temporary solution)
                    byte[] fileBytes = file.getBytes();
                    String base64Image = Base64.getEncoder().encodeToString(fileBytes);
                    String dataUrl = "data:" + file.getContentType() + ";base64," + base64Image;
                    photoUrls.add(dataUrl);
                }
            }
        }

        return photoUrls;
    }

    @GetMapping("/{id}/photos")
    public ResponseEntity<List<String>> getTireRequestPhotos(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request != null && request.getPhotoUrls() != null) {
                return ResponseEntity.ok(request.getPhotoUrls());
            } else {
                return ResponseEntity.ok(new ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("Error getting photos for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
}
