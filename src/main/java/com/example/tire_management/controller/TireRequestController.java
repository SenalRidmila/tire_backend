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
import org.springframework.http.CacheControl;

import java.util.concurrent.TimeUnit;

import com.example.tire_management.model.TireRequest;
import com.example.tire_management.service.TireRequestService;
import com.example.tire_management.service.TireRequestValidationService;
import com.example.tire_management.service.AzureTokenValidationService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tire-requests")
public class TireRequestController {

    private static final Logger logger = LoggerFactory.getLogger(TireRequestController.class);

    @Autowired
    private TireRequestService tireRequestService;
    
    @Autowired
    private TireRequestValidationService validationService;
    
    @Autowired
    private AzureTokenValidationService azureTokenValidationService;

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

    // Fast endpoint for table loading without photos - OPTIMIZED
    @GetMapping("/fast")
    public ResponseEntity<Map<String, Object>> getAllTireRequestsFast(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "false") boolean includePhotos) {
        
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("id").descending());
            
            if (includePhotos) {
                org.springframework.data.domain.Page<TireRequest> requestPage = 
                    tireRequestService.getRequestsPaginated(pageable);
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", requestPage.getContent());
                response.put("totalElements", requestPage.getTotalElements());
                response.put("totalPages", requestPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasPhotos", true);
                
                return ResponseEntity.ok(response);
            } else {
                // Ultra-fast path without any photo processing
                org.springframework.data.domain.Page<TireRequest> requestPage = 
                    tireRequestService.getRequestsPaginatedWithoutPhotos(pageable);
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", requestPage.getContent());
                response.put("totalElements", requestPage.getTotalElements());
                response.put("totalPages", requestPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasPhotos", false);
                response.put("optimized", true);
                
                logger.info("Ultra-fast load: {} requests on page {} of {} (no photo processing)", 
                           requestPage.getContent().size(), page + 1, requestPage.getTotalPages());
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(45, TimeUnit.SECONDS)
                        .cachePublic())
                    .body(response);
            }
        } catch (Exception e) {
            logger.error("Error in fast load: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Ultra-fast summary endpoint for dashboard counts only
    @GetMapping("/summary/counts")
    public ResponseEntity<Map<String, Object>> getDashboardCounts() {
        try {
            Map<String, Object> counts = new HashMap<>();
            
            // Manager counts
            long managerCount = tireRequestService.getRequestsCountByStatuses(
                List.of("pending", "PENDING", "MANAGER_APPROVED", "APPROVED"));
            counts.put("managerRequests", managerCount);
            
            // TTO counts
            long ttoCount = tireRequestService.getRequestsCountByStatuses(
                List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved", "pending"));
            counts.put("ttoRequests", ttoCount);
            
            // Engineer counts
            long engineerCount = tireRequestService.getRequestsCountByStatuses(
                List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED"));
            counts.put("engineerRequests", engineerCount);
            
            // Total count
            long totalCount = tireRequestService.getTotalRequestsCount();
            counts.put("totalRequests", totalCount);
            
            counts.put("timestamp", new Date().toString());
            
            logger.info("Dashboard counts: Manager={}, TTO={}, Engineer={}, Total={}", 
                       managerCount, ttoCount, engineerCount, totalCount);
            
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS)
                    .cachePublic())
                .body(counts);
        } catch (Exception e) {
            logger.error("Error getting dashboard counts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Ultra-fast table data endpoint for CRUD tables - minimal data only
    @GetMapping("/table/minimal")
    public ResponseEntity<Map<String, Object>> getTableDataMinimal(
            @RequestParam(defaultValue = "all") String dashboard,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("id").descending());
            
            List<String> statuses;
            switch (dashboard.toLowerCase()) {
                case "manager":
                    statuses = List.of("pending", "PENDING", "MANAGER_APPROVED", "APPROVED");
                    break;
                case "tto":
                    statuses = List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved", "pending");
                    break;
                case "engineer":
                    statuses = List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED");
                    break;
                default:
                    statuses = List.of("pending", "PENDING", "MANAGER_APPROVED", "APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved");
            }
            
            // Get data without photos for ultra-fast loading
            org.springframework.data.domain.Page<TireRequest> requestPage = 
                tireRequestService.getRequestsByStatusesPaginatedWithoutPhotos(statuses, pageable);
            
            // Create minimal response with only essential CRUD table data
            List<Map<String, Object>> minimalRequests = new ArrayList<>();
            for (TireRequest request : requestPage.getContent()) {
                Map<String, Object> minimal = new HashMap<>();
                minimal.put("id", request.getId());
                minimal.put("vehicleNo", request.getVehicleNo());
                minimal.put("vehicleType", request.getVehicleType());
                minimal.put("vehicleBrand", request.getVehicleBrand());
                minimal.put("status", request.getStatus());
                minimal.put("email", request.getemail());
                minimal.put("officerServiceNo", request.getOfficerServiceNo());
                minimal.put("tireSize", request.getTireSize());
                minimal.put("replacementDate", request.getReplacementDate());
                
                // Quick photo count check without loading photos
                int photoCount = 0;
                if (request.getTirePhotoUrls() != null) {
                    photoCount = request.getTirePhotoUrls().size();
                } else if (request.getPhotoUrls() != null) {
                    photoCount = request.getPhotoUrls().size();
                }
                minimal.put("photoCount", photoCount);
                minimal.put("hasPhotos", photoCount > 0);
                
                minimalRequests.add(minimal);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", minimalRequests);
            response.put("totalElements", requestPage.getTotalElements());
            response.put("totalPages", requestPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            response.put("dashboard", dashboard);
            response.put("dataType", "minimal");
            response.put("loadTime", "ultraFast");
            
            logger.info("Ultra-fast table load for {}: {} records on page {} of {} ({}ms response)", 
                       dashboard, minimalRequests.size(), page + 1, requestPage.getTotalPages(), 
                       System.currentTimeMillis() % 1000);
            
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS)
                    .cachePublic())
                .body(response);
                
        } catch (Exception e) {
            logger.error("Error in ultra-fast table load for {}: {}", dashboard, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
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
        int totalPhotos = 0;
        for (TireRequest request : requests) {
            consolidatePhotos(request);
            
            // Count photos for logging
            if (request.getTirePhotoUrls() != null) {
                totalPhotos += request.getTirePhotoUrls().size();
            }
        }
        
        logger.info("Retrieved {} manager requests with {} total photos", requests.size(), totalPhotos);
        return requests;
    }

    // Optimized manager dashboard for ultra-fast table loading
    @GetMapping("/manager/requests/fast")
    public ResponseEntity<Map<String, Object>> getRequestsForManagerFast(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "false") boolean includePhotos) {
        
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("id").descending());
            
            List<String> statuses = List.of("pending", "PENDING", "MANAGER_APPROVED", "APPROVED");
            
            if (includePhotos) {
                org.springframework.data.domain.Page<TireRequest> requestPage = 
                    tireRequestService.getRequestsByStatusesPaginated(statuses, pageable);
                
                // Minimal photo processing for faster response
                List<TireRequest> processedRequests = new ArrayList<>();
                int totalPhotos = 0;
                int requestsWithPhotos = 0;
                
                for (TireRequest request : requestPage.getContent()) {
                    // Quick photo check without consolidation for speed
                    if (request.getTirePhotoUrls() != null && !request.getTirePhotoUrls().isEmpty()) {
                        requestsWithPhotos++;
                        totalPhotos += request.getTirePhotoUrls().size();
                    }
                    processedRequests.add(request);
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", processedRequests);
                response.put("totalElements", requestPage.getTotalElements());
                response.put("totalPages", requestPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasPhotos", true);
                response.put("totalPhotos", totalPhotos);
                response.put("requestsWithPhotos", requestsWithPhotos);
                response.put("dashboard", "Manager");
                response.put("optimized", true);
                
                logger.info("Manager fast load with photos: {} requests on page {} of {}, {} photos total", 
                           processedRequests.size(), page + 1, requestPage.getTotalPages(), totalPhotos);
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                    .body(response);
            } else {
                // Ultra-fast path without photo processing
                org.springframework.data.domain.Page<TireRequest> requestPage = 
                    tireRequestService.getRequestsByStatusesPaginatedWithoutPhotos(statuses, pageable);
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", requestPage.getContent());
                response.put("totalElements", requestPage.getTotalElements());
                response.put("totalPages", requestPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasPhotos", false);
                response.put("dashboard", "Manager");
                response.put("ultraFast", true);
                
                logger.info("Manager ultra-fast load: {} requests on page {} of {} (no processing)", 
                           requestPage.getContent().size(), page + 1, requestPage.getTotalPages());
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(45, TimeUnit.SECONDS)
                        .cachePublic())
                    .body(response);
            }
        } catch (Exception e) {
            logger.error("Error in manager fast load: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // TTO dashboard – DO NOT hide after TTO action; show all relevant
    @GetMapping("/tto/requests")
    public List<TireRequest> getRequestsForTTO() {
        List<TireRequest> requests = tireRequestService.getRequestsByStatuses(
                List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved", "pending")
        );
        
        // Ensure photos are properly consolidated for each request for TTO dashboard display
        int totalPhotos = 0;
        for (TireRequest request : requests) {
            consolidatePhotos(request);
            
            // Count photos for logging
            if (request.getTirePhotoUrls() != null) {
                totalPhotos += request.getTirePhotoUrls().size();
            }
        }
        
        logger.info("Retrieved {} TTO requests with {} total photos for CRUD table display", requests.size(), totalPhotos);
        return requests;
    }

    // Enhanced TTO dashboard endpoint with photos included for CRUD table
    @GetMapping("/tto/requests-with-photos")
    public ResponseEntity<Map<String, Object>> getTTORequestsWithPhotosForTable() {
        try {
            List<TireRequest> requests = tireRequestService.getRequestsByStatuses(
                    List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved", "pending")
            );
            
            int totalRequests = requests.size();
            int totalPhotos = 0;
            int requestsWithPhotos = 0;
            
            for (TireRequest request : requests) {
                consolidatePhotos(request);
                
                // Validate photos for each request and make them available in CRUD table
                if (request.getTirePhotoUrls() != null && !request.getTirePhotoUrls().isEmpty()) {
                    List<String> validPhotos = new ArrayList<>();
                    for (String photo : request.getTirePhotoUrls()) {
                        if (isValidBase64Image(photo)) {
                            validPhotos.add(photo);
                        } else {
                            logger.warn("Invalid photo detected in TTO request {}", request.getId());
                        }
                    }
                    request.setTirePhotoUrls(validPhotos);
                    request.setPhotoUrls(validPhotos);
                    
                    if (!validPhotos.isEmpty()) {
                        requestsWithPhotos++;
                        totalPhotos += validPhotos.size();
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests);
            response.put("totalRequests", totalRequests);
            response.put("requestsWithPhotos", requestsWithPhotos);
            response.put("totalPhotos", totalPhotos);
            response.put("dashboard", "TTO");
            response.put("tableView", "withPhotos");
            
            logger.info("TTO CRUD table: {} requests, {} with photos, {} total photos displayed", 
                       totalRequests, requestsWithPhotos, totalPhotos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting TTO requests with photos for table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Optimized TTO dashboard for ultra-fast table loading
    @GetMapping("/tto/requests/fast")
    public ResponseEntity<Map<String, Object>> getRequestsForTTOFast(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "false") boolean includePhotos) {
        
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("id").descending());
            
            List<String> statuses = List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved", "pending");
            
            if (includePhotos) {
                org.springframework.data.domain.Page<TireRequest> requestPage = 
                    tireRequestService.getRequestsByStatusesPaginated(statuses, pageable);
                
                // Minimal photo processing for faster response
                List<TireRequest> processedRequests = new ArrayList<>();
                int totalPhotos = 0;
                int requestsWithPhotos = 0;
                Map<String, Integer> photoCountMap = new HashMap<>();
                
                for (TireRequest request : requestPage.getContent()) {
                    // Quick photo check without full validation for speed
                    int photoCount = 0;
                    if (request.getTirePhotoUrls() != null && !request.getTirePhotoUrls().isEmpty()) {
                        photoCount = request.getTirePhotoUrls().size();
                        requestsWithPhotos++;
                        totalPhotos += photoCount;
                    }
                    photoCountMap.put(request.getId(), photoCount);
                    processedRequests.add(request);
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", processedRequests);
                response.put("totalElements", requestPage.getTotalElements());
                response.put("totalPages", requestPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasPhotos", true);
                response.put("totalPhotos", totalPhotos);
                response.put("requestsWithPhotos", requestsWithPhotos);
                response.put("dashboard", "TTO");
                response.put("photoViewEnabled", true);
                response.put("photoCountMapping", photoCountMap);
                response.put("optimized", true);
                
                logger.info("TTO fast load with photos: {} requests on page {} of {}, {} photos total", 
                           processedRequests.size(), page + 1, requestPage.getTotalPages(), totalPhotos);
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                    .body(response);
            } else {
                // Ultra-fast path without photo processing
                org.springframework.data.domain.Page<TireRequest> requestPage = 
                    tireRequestService.getRequestsByStatusesPaginatedWithoutPhotos(statuses, pageable);
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", requestPage.getContent());
                response.put("totalElements", requestPage.getTotalElements());
                response.put("totalPages", requestPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasPhotos", false);
                response.put("dashboard", "TTO");
                response.put("ultraFast", true);
                
                logger.info("TTO ultra-fast load: {} requests on page {} of {} (no processing)", 
                           requestPage.getContent().size(), page + 1, requestPage.getTotalPages());
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(45, TimeUnit.SECONDS)
                        .cachePublic())
                    .body(response);
            }
        } catch (Exception e) {
            logger.error("Error in TTO fast load: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Engineer dashboard – Enhanced with better error handling and TTO approval support
    @GetMapping("/engineer/requests")
    public ResponseEntity<Map<String, Object>> getRequestsForEngineer() {
        try {
            List<TireRequest> requests = tireRequestService.getRequestsByStatuses(
                List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED"));
            
            // Ensure photos are properly consolidated for each request
            int totalPhotos = 0;
            int ttoApprovedCount = 0;
            for (TireRequest request : requests) {
                consolidatePhotos(request);
                
                // Count TTO approved requests
                if ("TTO_APPROVED".equals(request.getStatus())) {
                    ttoApprovedCount++;
                }
                
                // Count photos for logging
                if (request.getTirePhotoUrls() != null) {
                    totalPhotos += request.getTirePhotoUrls().size();
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests);
            response.put("totalRequests", requests.size());
            response.put("ttoApprovedCount", ttoApprovedCount);
            response.put("totalPhotos", totalPhotos);
            response.put("dashboard", "Engineer");
            response.put("status", "success");
            
            logger.info("Engineer dashboard: Retrieved {} requests ({} TTO approved) with {} total photos", 
                       requests.size(), ttoApprovedCount, totalPhotos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in engineer dashboard: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", "Failed to load engineer dashboard: " + e.getMessage());
            errorResponse.put("requests", new ArrayList<>());
            errorResponse.put("totalRequests", 0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Optimized Engineer dashboard for ultra-fast table loading
    @GetMapping("/engineer/requests/fast")
    public ResponseEntity<Map<String, Object>> getRequestsForEngineerFast(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "false") boolean includePhotos) {
        
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("id").descending());
            
            List<String> statuses = List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED");
            
            logger.info("Engineer fast load request: page={}, size={}, includePhotos={}", page, size, includePhotos);
            
            if (includePhotos) {
                org.springframework.data.domain.Page<TireRequest> requestPage = 
                    tireRequestService.getRequestsByStatusesPaginated(statuses, pageable);
                
                // Count TTO approved requests for debugging
                int ttoApprovedCount = 0;
                for (TireRequest request : requestPage.getContent()) {
                    if ("TTO_APPROVED".equals(request.getStatus())) {
                        ttoApprovedCount++;
                    }
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", requestPage.getContent());
                response.put("totalElements", requestPage.getTotalElements());
                response.put("totalPages", requestPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasPhotos", true);
                response.put("dashboard", "Engineer");
                response.put("optimized", true);
                response.put("ttoApprovedCount", ttoApprovedCount);
                response.put("statusFilter", statuses);
                
                logger.info("Engineer fast load with photos: {} requests on page {} of {} ({} TTO approved)", 
                           requestPage.getContent().size(), page + 1, requestPage.getTotalPages(), ttoApprovedCount);
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                    .body(response);
            } else {
                // Ultra-fast path without photo processing
                org.springframework.data.domain.Page<TireRequest> requestPage = 
                    tireRequestService.getRequestsByStatusesPaginatedWithoutPhotos(statuses, pageable);
                
                // Count TTO approved requests for debugging
                int ttoApprovedCount = 0;
                for (TireRequest request : requestPage.getContent()) {
                    if ("TTO_APPROVED".equals(request.getStatus())) {
                        ttoApprovedCount++;
                    }
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", requestPage.getContent());
                response.put("totalElements", requestPage.getTotalElements());
                response.put("totalPages", requestPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);
                response.put("hasPhotos", false);
                response.put("dashboard", "Engineer");
                response.put("ultraFast", true);
                response.put("ttoApprovedCount", ttoApprovedCount);
                response.put("statusFilter", statuses);
                
                logger.info("Engineer ultra-fast load: {} requests on page {} of {} ({} TTO approved, no processing)", 
                           requestPage.getContent().size(), page + 1, requestPage.getTotalPages(), ttoApprovedCount);
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(45, TimeUnit.SECONDS)
                        .cachePublic())
                    .body(response);
            }
        } catch (Exception e) {
            logger.error("Error in Engineer fast load: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", "Failed to load engineer requests: " + e.getMessage());
            errorResponse.put("content", new ArrayList<>());
            errorResponse.put("totalElements", 0);
            errorResponse.put("totalPages", 0);
            errorResponse.put("dashboard", "Engineer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Engineer debug endpoint to check TTO approved requests
    @GetMapping("/engineer/debug/tto-approved")
    public ResponseEntity<Map<String, Object>> debugTTOApprovedForEngineer() {
        try {
            List<TireRequest> allTTOApproved = tireRequestService.getRequestsByStatuses(List.of("TTO_APPROVED"));
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("totalTTOApproved", allTTOApproved.size());
            debug.put("requests", allTTOApproved);
            debug.put("timestamp", new Date().toString());
            debug.put("engineerCanView", true);
            
            // Log some details
            for (TireRequest request : allTTOApproved) {
                logger.info("TTO Approved Request for Engineer: ID={}, Vehicle={}, Status={}, TTO Date={}", 
                           request.getId(), request.getVehicleNo(), request.getStatus(), request.getTtoApprovalDate());
            }
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("Error in engineer TTO debug: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Additional endpoint for manager dashboard with photo validation
    @GetMapping("/manager/requests-with-photos")
    public ResponseEntity<Map<String, Object>> getManagerRequestsWithPhotos() {
        try {
            List<TireRequest> requests = tireRequestService.getRequestsByStatuses(List.of("pending", "PENDING", "MANAGER_APPROVED", "APPROVED"));
            
            int totalRequests = requests.size();
            int totalPhotos = 0;
            int requestsWithPhotos = 0;
            
            for (TireRequest request : requests) {
                consolidatePhotos(request);
                
                // Validate photos for each request
                if (request.getTirePhotoUrls() != null && !request.getTirePhotoUrls().isEmpty()) {
                    List<String> validPhotos = new ArrayList<>();
                    for (String photo : request.getTirePhotoUrls()) {
                        if (isValidBase64Image(photo)) {
                            validPhotos.add(photo);
                        } else {
                            logger.warn("Invalid photo detected in request {}", request.getId());
                        }
                    }
                    request.setTirePhotoUrls(validPhotos);
                    request.setPhotoUrls(validPhotos);
                    
                    if (!validPhotos.isEmpty()) {
                        requestsWithPhotos++;
                        totalPhotos += validPhotos.size();
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests);
            response.put("totalRequests", totalRequests);
            response.put("requestsWithPhotos", requestsWithPhotos);
            response.put("totalPhotos", totalPhotos);
            
            logger.info("Manager dashboard: {} requests, {} with photos, {} total photos", 
                       totalRequests, requestsWithPhotos, totalPhotos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting manager requests with photos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // TTO specific endpoint to view photos for a request in CRUD table
    @GetMapping("/tto/{id}/photos")
    public ResponseEntity<Map<String, Object>> getTTORequestPhotos(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Ensure photos are consolidated and validated for TTO CRUD table display
            consolidatePhotos(request);
            
            List<String> validPhotos = new ArrayList<>();
            if (request.getTirePhotoUrls() != null) {
                for (String photo : request.getTirePhotoUrls()) {
                    if (isValidBase64Image(photo)) {
                        validPhotos.add(photo);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", id);
            response.put("vehicleNo", request.getVehicleNo());
            response.put("vehicleType", request.getVehicleType());
            response.put("vehicleBrand", request.getVehicleBrand());
            response.put("photos", validPhotos);
            response.put("photoCount", validPhotos.size());
            response.put("status", request.getStatus());
            response.put("viewedBy", "TTO");
            response.put("tableDisplay", true);
            response.put("hasValidPhotos", !validPhotos.isEmpty());
            
            logger.info("TTO CRUD table viewing {} photos for request {} (Vehicle: {})", 
                       validPhotos.size(), id, request.getVehicleNo());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting TTO photos for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // TTO CRUD table photo thumbnails endpoint for better performance
    @GetMapping("/tto/{id}/photos/thumbnails")
    public ResponseEntity<Map<String, Object>> getTTORequestPhotoThumbnails(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Ensure photos are consolidated and validated
            consolidatePhotos(request);
            
            List<Map<String, Object>> photoThumbnails = new ArrayList<>();
            if (request.getTirePhotoUrls() != null) {
                for (int i = 0; i < request.getTirePhotoUrls().size(); i++) {
                    String photo = request.getTirePhotoUrls().get(i);
                    if (isValidBase64Image(photo)) {
                        Map<String, Object> thumbnail = new HashMap<>();
                        thumbnail.put("index", i);
                        thumbnail.put("preview", photo.length() > 100 ? photo.substring(0, 100) + "..." : photo);
                        thumbnail.put("hasFullImage", true);
                        thumbnail.put("size", photo.length());
                        photoThumbnails.add(thumbnail);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", id);
            response.put("vehicleNo", request.getVehicleNo());
            response.put("thumbnails", photoThumbnails);
            response.put("thumbnailCount", photoThumbnails.size());
            response.put("status", request.getStatus());
            response.put("viewType", "thumbnails");
            response.put("tableDisplay", true);
            
            logger.info("TTO CRUD table thumbnails: {} thumbnails for request {} (Vehicle: {})", 
                       photoThumbnails.size(), id, request.getVehicleNo());
            
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS))
                .body(response);
        } catch (Exception e) {
            logger.error("Error getting TTO photo thumbnails for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // TTO CRUD table single photo endpoint for modal or full view
    @GetMapping("/tto/{id}/photos/{photoIndex}")
    public ResponseEntity<Map<String, Object>> getTTORequestSinglePhoto(
            @PathVariable String id, 
            @PathVariable int photoIndex) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Ensure photos are consolidated and validated
            consolidatePhotos(request);
            
            List<String> validPhotos = new ArrayList<>();
            if (request.getTirePhotoUrls() != null) {
                for (String photo : request.getTirePhotoUrls()) {
                    if (isValidBase64Image(photo)) {
                        validPhotos.add(photo);
                    }
                }
            }
            
            if (photoIndex < 0 || photoIndex >= validPhotos.size()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid photo index", "availablePhotos", validPhotos.size()));
            }
            
            String selectedPhoto = validPhotos.get(photoIndex);
            
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", id);
            response.put("vehicleNo", request.getVehicleNo());
            response.put("photoIndex", photoIndex);
            response.put("photo", selectedPhoto);
            response.put("totalPhotos", validPhotos.size());
            response.put("status", request.getStatus());
            response.put("viewType", "singlePhoto");
            response.put("tableDisplay", true);
            
            logger.info("TTO CRUD table single photo: index {} for request {} (Vehicle: {})", 
                       photoIndex, id, request.getVehicleNo());
            
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS))
                .body(response);
        } catch (Exception e) {
            logger.error("Error getting TTO single photo for request {} index {}: {}", id, photoIndex, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // TTO dashboard CRUD table photo status overview
    @GetMapping("/tto/requests/photo-status")
    public ResponseEntity<Map<String, Object>> getTTOPhotoStatusOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("id").descending());
            
            List<String> statuses = List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved", "pending");
            org.springframework.data.domain.Page<TireRequest> requestPage = 
                tireRequestService.getRequestsByStatusesPaginated(statuses, pageable);
            
            List<Map<String, Object>> photoStatusList = new ArrayList<>();
            int totalPhotos = 0;
            int requestsWithPhotos = 0;
            
            for (TireRequest request : requestPage.getContent()) {
                consolidatePhotos(request);
                
                Map<String, Object> requestStatus = new HashMap<>();
                requestStatus.put("requestId", request.getId());
                requestStatus.put("vehicleNo", request.getVehicleNo());
                requestStatus.put("status", request.getStatus());
                
                List<String> validPhotos = new ArrayList<>();
                if (request.getTirePhotoUrls() != null) {
                    for (String photo : request.getTirePhotoUrls()) {
                        if (isValidBase64Image(photo)) {
                            validPhotos.add(photo);
                        }
                    }
                }
                
                requestStatus.put("photoCount", validPhotos.size());
                requestStatus.put("hasPhotos", !validPhotos.isEmpty());
                requestStatus.put("photoViewUrl", "/api/tire-requests/tto/" + request.getId() + "/photos");
                requestStatus.put("thumbnailUrl", "/api/tire-requests/tto/" + request.getId() + "/photos/thumbnails");
                
                if (!validPhotos.isEmpty()) {
                    requestsWithPhotos++;
                    totalPhotos += validPhotos.size();
                }
                
                photoStatusList.add(requestStatus);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", photoStatusList);
            response.put("totalElements", requestPage.getTotalElements());
            response.put("totalPages", requestPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            response.put("totalPhotos", totalPhotos);
            response.put("requestsWithPhotos", requestsWithPhotos);
            response.put("dashboard", "TTO");
            response.put("viewType", "photoStatus");
            response.put("tableDisplayReady", true);
            
            logger.info("TTO photo status overview: {} requests, {} with photos ({} total photos)", 
                       photoStatusList.size(), requestsWithPhotos, totalPhotos);
            
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(120, TimeUnit.SECONDS))
                .body(response);
        } catch (Exception e) {
            logger.error("Error getting TTO photo status overview: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // TTO bulk photo validation endpoint
    @PostMapping("/tto/validate-all-photos")
    public ResponseEntity<Map<String, Object>> validateAllTTOPhotos() {
        try {
            List<TireRequest> requests = tireRequestService.getRequestsByStatuses(
                    List.of("APPROVED", "MANAGER_APPROVED", "TTO_APPROVED", "TTO_REJECTED", "ENGINEER_APPROVED", "ENGINEER_REJECTED", "approved", "pending")
            );
            
            int totalRequests = requests.size();
            int requestsProcessed = 0;
            int totalPhotosValidated = 0;
            int corruptedPhotosRemoved = 0;
            
            for (TireRequest request : requests) {
                List<String> validPhotos = new ArrayList<>();
                
                // Validate and consolidate photos
                consolidatePhotos(request);
                
                if (request.getTirePhotoUrls() != null) {
                    for (String photo : request.getTirePhotoUrls()) {
                        if (isValidBase64Image(photo)) {
                            validPhotos.add(photo);
                        } else {
                            corruptedPhotosRemoved++;
                        }
                    }
                }
                
                // Update request with validated photos
                request.setTirePhotoUrls(validPhotos);
                request.setPhotoUrls(validPhotos);
                tireRequestService.updateTireRequest(request.getId(), request);
                
                requestsProcessed++;
                totalPhotosValidated += validPhotos.size();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalRequests", totalRequests);
            result.put("requestsProcessed", requestsProcessed);
            result.put("totalPhotosValidated", totalPhotosValidated);
            result.put("corruptedPhotosRemoved", corruptedPhotosRemoved);
            result.put("processedBy", "TTO");
            result.put("timestamp", new Date().toString());
            
            logger.info("TTO bulk photo validation: {} requests, {} photos validated, {} corrupted removed", 
                       requestsProcessed, totalPhotosValidated, corruptedPhotosRemoved);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in TTO bulk photo validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Engineer dashboard with photo validation
    @GetMapping("/engineer/requests-with-photos")
    public ResponseEntity<Map<String, Object>> getEngineerRequestsWithPhotos() {
        try {
            List<TireRequest> requests = tireRequestService.getRequestsByStatuses(
                    List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED")
            );
            
            int totalRequests = requests.size();
            int totalPhotos = 0;
            int requestsWithPhotos = 0;
            
            for (TireRequest request : requests) {
                consolidatePhotos(request);
                
                // Validate photos for each request
                if (request.getTirePhotoUrls() != null && !request.getTirePhotoUrls().isEmpty()) {
                    List<String> validPhotos = new ArrayList<>();
                    for (String photo : request.getTirePhotoUrls()) {
                        if (isValidBase64Image(photo)) {
                            validPhotos.add(photo);
                        } else {
                            logger.warn("Invalid photo detected in request {}", request.getId());
                        }
                    }
                    request.setTirePhotoUrls(validPhotos);
                    request.setPhotoUrls(validPhotos);
                    
                    if (!validPhotos.isEmpty()) {
                        requestsWithPhotos++;
                        totalPhotos += validPhotos.size();
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", requests);
            response.put("totalRequests", totalRequests);
            response.put("requestsWithPhotos", requestsWithPhotos);
            response.put("totalPhotos", totalPhotos);
            
            logger.info("Engineer dashboard: {} requests, {} with photos, {} total photos", 
                       totalRequests, requestsWithPhotos, totalPhotos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting Engineer requests with photos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Engineer specific endpoint to view photos for a request
    @GetMapping("/engineer/{id}/photos")
    public ResponseEntity<Map<String, Object>> getEngineerRequestPhotos(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if engineer has access to this request
            if (!List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED").contains(request.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Engineer access denied for this request status"));
            }
            
            // Ensure photos are consolidated and validated
            consolidatePhotos(request);
            
            List<String> validPhotos = new ArrayList<>();
            if (request.getTirePhotoUrls() != null) {
                for (String photo : request.getTirePhotoUrls()) {
                    if (isValidBase64Image(photo)) {
                        validPhotos.add(photo);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", id);
            response.put("vehicleNo", request.getVehicleNo());
            response.put("vehicleType", request.getVehicleType());
            response.put("vehicleBrand", request.getVehicleBrand());
            response.put("tireSize", request.getTireSize());
            response.put("photos", validPhotos);
            response.put("photoCount", validPhotos.size());
            response.put("status", request.getStatus());
            response.put("viewedBy", "Engineer");
            response.put("ttoApprovalDate", request.getTtoApprovalDate());
            
            logger.info("Engineer viewing {} photos for request {} (Vehicle: {} - {})", 
                       validPhotos.size(), id, request.getVehicleNo(), request.getVehicleType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting Engineer photos for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Engineer bulk photo validation endpoint
    @PostMapping("/engineer/validate-all-photos")
    public ResponseEntity<Map<String, Object>> validateAllEngineerPhotos() {
        try {
            List<TireRequest> requests = tireRequestService.getRequestsByStatuses(
                    List.of("TTO_APPROVED", "ENGINEER_APPROVED", "ENGINEER_REJECTED")
            );
            
            int totalRequests = requests.size();
            int requestsProcessed = 0;
            int totalPhotosValidated = 0;
            int corruptedPhotosRemoved = 0;
            
            for (TireRequest request : requests) {
                List<String> validPhotos = new ArrayList<>();
                
                // Validate and consolidate photos
                consolidatePhotos(request);
                
                if (request.getTirePhotoUrls() != null) {
                    for (String photo : request.getTirePhotoUrls()) {
                        if (isValidBase64Image(photo)) {
                            validPhotos.add(photo);
                        } else {
                            corruptedPhotosRemoved++;
                        }
                    }
                }
                
                // Update request with validated photos
                request.setTirePhotoUrls(validPhotos);
                request.setPhotoUrls(validPhotos);
                tireRequestService.updateTireRequest(request.getId(), request);
                
                requestsProcessed++;
                totalPhotosValidated += validPhotos.size();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalRequests", totalRequests);
            result.put("requestsProcessed", requestsProcessed);
            result.put("totalPhotosValidated", totalPhotosValidated);
            result.put("corruptedPhotosRemoved", corruptedPhotosRemoved);
            result.put("processedBy", "Engineer");
            result.put("timestamp", new Date().toString());
            
            logger.info("Engineer bulk photo validation: {} requests, {} photos validated, {} corrupted removed", 
                       requestsProcessed, totalPhotosValidated, corruptedPhotosRemoved);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in Engineer bulk photo validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }



    // ----------------- Create / Update / Delete -----------------
    @PostMapping
    public ResponseEntity<?> createTireRequest(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "tirePhotos", required = false) List<MultipartFile> tirePhotos) {

        try {
            TireRequest request = buildTireRequestFromParams(params);
            
            // Auto-populate required fields
            request = validationService.autoPopulateFields(request);
            
            // Validate the request
            List<String> validationErrors = validationService.validateTireRequest(request);
            
            // Validate images if provided
            if (tirePhotos != null && !tirePhotos.isEmpty()) {
                MultipartFile[] filesArray = tirePhotos.toArray(new MultipartFile[0]);
                List<String> imageErrors = validationService.validateImageFiles(filesArray);
                validationErrors.addAll(imageErrors);
            }
            
            // If validation errors exist, return them
            if (!validationErrors.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("errors", validationErrors);
                errorResponse.put("message", "Validation failed");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<String> photoUrls = saveUploadedFiles(tirePhotos);
            // Save to both photo fields for consistency
            request.setTirePhotoUrls(photoUrls);
            request.setPhotoUrls(photoUrls);

            TireRequest createdRequest = tireRequestService.createTireRequest(request);
            logger.info("Created tire request with {} photos", photoUrls.size());
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("data", createdRequest);
            successResponse.put("message", "Tire request created successfully");
            return ResponseEntity.ok(successResponse);
        } catch (IOException e) {
            logger.error("Error creating tire request: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error processing file uploads: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error creating tire request: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Alternative endpoint for form-based submission (matching frontend)
    @PostMapping("/create")
    public ResponseEntity<?> createTireRequestForm(
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
            
            // Auto-populate required fields
            request = validationService.autoPopulateFields(request);
            
            // Validate the request
            List<String> validationErrors = validationService.validateTireRequest(request);
            
            // Validate images if provided
            if (photos != null && photos.length > 0) {
                List<String> imageErrors = validationService.validateImageFiles(photos);
                validationErrors.addAll(imageErrors);
            }
            
            // If validation errors exist, return them
            if (!validationErrors.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("errors", validationErrors);
                errorResponse.put("message", "Validation failed");
                return ResponseEntity.badRequest().body(errorResponse);
            }

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
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("data", savedRequest);
            successResponse.put("message", "Tire request created successfully");
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            logger.error("Error creating tire request: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error creating tire request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTireRequest(
            @PathVariable String id,
            @RequestParam Map<String, String> params,
            @RequestParam(value = "tirePhotos", required = false) List<MultipartFile> tirePhotos) {

        try {
            TireRequest request = buildTireRequestFromParams(params);
            
            // Auto-populate required fields
            request = validationService.autoPopulateFields(request);
            
            // Validate the request
            List<String> validationErrors = validationService.validateTireRequest(request);
            
            // Validate images if provided
            if (tirePhotos != null && !tirePhotos.isEmpty()) {
                MultipartFile[] filesArray = tirePhotos.toArray(new MultipartFile[0]);
                List<String> imageErrors = validationService.validateImageFiles(filesArray);
                validationErrors.addAll(imageErrors);
            }
            
            // If validation errors exist, return them
            if (!validationErrors.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("errors", validationErrors);
                errorResponse.put("message", "Validation failed");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<String> photoUrls = saveUploadedFiles(tirePhotos);
            // Save to both photo fields for consistency
            request.setTirePhotoUrls(photoUrls);
            request.setPhotoUrls(photoUrls);

            TireRequest updatedRequest = tireRequestService.updateTireRequest(id, request);
            logger.info("Updated tire request {} with {} photos", id, photoUrls.size());
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("data", updatedRequest);
            successResponse.put("message", "Tire request updated successfully");
            return ResponseEntity.ok(successResponse);
        } catch (IOException e) {
            logger.error("Error updating tire request {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error processing file uploads: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error updating tire request {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ----------------- Validation Endpoints -----------------
    
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateTireRequest(@RequestBody TireRequest request) {
        try {
            // Auto-populate required fields
            request = validationService.autoPopulateFields(request);
            
            // Validate the request
            List<String> validationErrors = validationService.validateTireRequest(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", validationErrors.isEmpty());
            response.put("errors", validationErrors);
            response.put("autoPopulatedData", request);
            
            if (validationErrors.isEmpty()) {
                response.put("message", "Validation successful");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Validation failed");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error during validation: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("message", "Validation service error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/validate-images")
    public ResponseEntity<Map<String, Object>> validateImages(
            @RequestParam("images") MultipartFile[] images) {
        try {
            List<String> imageErrors = validationService.validateImageFiles(images);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", imageErrors.isEmpty());
            response.put("errors", imageErrors);
            
            if (imageErrors.isEmpty()) {
                response.put("message", "All images are valid");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Image validation failed");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error during image validation: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("message", "Image validation service error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    public ResponseEntity<Map<String, Object>> approveTireRequestByTTO(@PathVariable String id) {
        try {
            TireRequest approvedRequest = tireRequestService.approveTireRequestByTTO(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Request approved by TTO and notification sent to Engineer");
            response.put("request", approvedRequest);
            response.put("status", approvedRequest.getStatus());
            response.put("ttoApprovalDate", approvedRequest.getTtoApprovalDate());
            response.put("nextStep", "Engineer Review");
            response.put("engineerNotified", true);
            
            logger.info("TTO approved request {} successfully, engineer notification sent", id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error in TTO approval for request {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to approve request: " + e.getMessage());
            errorResponse.put("error", true);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error in TTO approval for request {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error during approval");
            errorResponse.put("error", true);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/tto-reject")
    public ResponseEntity<Map<String, Object>> rejectTireRequestByTTO(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Rejection reason is required");
                errorResponse.put("error", true);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            TireRequest rejectedRequest = tireRequestService.rejectTireRequestByTTO(id, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Request rejected by TTO");
            response.put("request", rejectedRequest);
            response.put("status", rejectedRequest.getStatus());
            response.put("rejectionReason", reason);
            response.put("ttoRejectionDate", rejectedRequest.getTtoRejectionDate());
            response.put("rejected", true);
            
            logger.info("TTO rejected request {} with reason: {}", id, reason);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error in TTO rejection for request {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to reject request: " + e.getMessage());
            errorResponse.put("error", true);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error in TTO rejection for request {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error during rejection");
            errorResponse.put("error", true);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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

    // Debug endpoint to check photo data in database
    @GetMapping("/{id}/debug-photos")
    public ResponseEntity<Map<String, Object>> debugPhotos(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("requestId", id);
            debug.put("tirePhotoUrls", request.getTirePhotoUrls());
            debug.put("photoUrls", request.getPhotoUrls());
            debug.put("tirePhotoUrlsCount", request.getTirePhotoUrls() != null ? request.getTirePhotoUrls().size() : 0);
            debug.put("photoUrlsCount", request.getPhotoUrls() != null ? request.getPhotoUrls().size() : 0);
            
            // Check if photos contain base64 data
            if (request.getTirePhotoUrls() != null && !request.getTirePhotoUrls().isEmpty()) {
                String firstPhoto = request.getTirePhotoUrls().get(0);
                debug.put("firstPhotoPreview", firstPhoto != null && firstPhoto.length() > 50 ? 
                    firstPhoto.substring(0, 50) + "..." : firstPhoto);
                debug.put("isBase64", firstPhoto != null && firstPhoto.startsWith("data:"));
            }
            
            logger.info("Debug photos for request {}: tirePhotos={}, photoUrls={}", 
                id, 
                request.getTirePhotoUrls() != null ? request.getTirePhotoUrls().size() : 0,
                request.getPhotoUrls() != null ? request.getPhotoUrls().size() : 0);
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("Error debugging photos for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Photo validation and cleanup endpoint
    @PostMapping("/{id}/validate-photos")
    public ResponseEntity<Map<String, Object>> validateAndCleanPhotos(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<String> validPhotos = new ArrayList<>();
            List<String> corruptedPhotos = new ArrayList<>();
            int originalCount = 0;
            
            // Check tirePhotoUrls
            if (request.getTirePhotoUrls() != null) {
                originalCount += request.getTirePhotoUrls().size();
                for (String photo : request.getTirePhotoUrls()) {
                    if (isValidBase64Image(photo)) {
                        validPhotos.add(photo);
                    } else {
                        corruptedPhotos.add(photo.length() > 50 ? photo.substring(0, 50) + "..." : photo);
                    }
                }
            }
            
            // Check photoUrls
            if (request.getPhotoUrls() != null) {
                originalCount += request.getPhotoUrls().size();
                for (String photo : request.getPhotoUrls()) {
                    if (!validPhotos.contains(photo) && isValidBase64Image(photo)) {
                        validPhotos.add(photo);
                    } else if (!validPhotos.contains(photo)) {
                        corruptedPhotos.add(photo.length() > 50 ? photo.substring(0, 50) + "..." : photo);
                    }
                }
            }
            
            // Update request with only valid photos
            request.setTirePhotoUrls(validPhotos);
            request.setPhotoUrls(validPhotos);
            tireRequestService.updateTireRequest(id, request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("requestId", id);
            result.put("originalPhotoCount", originalCount);
            result.put("validPhotoCount", validPhotos.size());
            result.put("corruptedPhotoCount", corruptedPhotos.size());
            result.put("corruptedPhotos", corruptedPhotos);
            result.put("cleaned", true);
            
            logger.info("Photo validation for request {}: {} valid, {} corrupted out of {} total", 
                       id, validPhotos.size(), corruptedPhotos.size(), originalCount);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error validating photos for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validate if a string is a valid Base64 encoded image
     */
    private boolean isValidBase64Image(String dataUrl) {
        if (dataUrl == null || dataUrl.trim().isEmpty()) {
            return false;
        }
        
        // Check if it's a proper data URL
        if (!dataUrl.startsWith("data:image/")) {
            return false;
        }
        
        // Extract the Base64 part
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex == -1 || commaIndex == dataUrl.length() - 1) {
            return false;
        }
        
        String base64Part = dataUrl.substring(commaIndex + 1);
        
        try {
            // Try to decode the Base64
            byte[] decodedBytes = Base64.getDecoder().decode(base64Part);
            
            // Check if decoded bytes represent a valid image
            return isValidImageFile(decodedBytes);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid Base64 encoding detected: {}", e.getMessage());
            return false;
        }
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

    // ----------------- Azure AD Authentication Endpoint -----------------
    @PostMapping("/auth/azure-login")
    public ResponseEntity<Map<String, Object>> azureLogin(@RequestBody Map<String, String> azureLoginRequest) {
        try {
            String accessToken = azureLoginRequest.get("accessToken");
            
            if (accessToken == null || accessToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Azure AD access token is required"));
            }
            
            logger.info("🔍 Processing Azure AD authentication...");
            
            // Validate Azure AD token and get user information
            Map<String, Object> validationResult = azureTokenValidationService.validateAzureToken(accessToken);
            
            if ((Boolean) validationResult.get("success")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = (Map<String, Object>) validationResult.get("user");
                
                // Generate application JWT token for session management
                String appToken = azureTokenValidationService.generateAppToken(userInfo);
                
                Map<String, Object> response = new HashMap<>(validationResult);
                if (appToken != null) {
                    response.put("token", appToken);
                }
                
                logger.info("✅ Azure AD authentication successful for user: {}", userInfo.get("name"));
                return ResponseEntity.ok(response);
            } else {
                logger.warn("❌ Azure AD authentication failed: {}", validationResult.get("message"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationResult);
            }
            
        } catch (Exception e) {
            logger.error("Error during Azure AD authentication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Azure AD authentication error"));
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
                        
                        // Get file bytes and validate
                        byte[] fileBytes = file.getBytes();
                        if (fileBytes.length == 0) {
                            logger.warn("Empty file detected: {}. Skipping.", file.getOriginalFilename());
                            continue;
                        }
                        
                        // Validate image format by checking file headers
                        if (!isValidImageFile(fileBytes)) {
                            logger.warn("Invalid image file detected: {}. Skipping.", file.getOriginalFilename());
                            continue;
                        }
                        
                        // Convert to Base64 for database storage
                        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
                        
                        // Validate Base64 encoding
                        if (base64Image == null || base64Image.trim().isEmpty()) {
                            logger.warn("Base64 encoding failed for file: {}. Skipping.", file.getOriginalFilename());
                            continue;
                        }
                        
                        String dataUrl = "data:" + contentType + ";base64," + base64Image;
                        
                        // Verify the data URL is properly formed
                        if (dataUrl.length() > 50 && dataUrl.startsWith("data:image/")) {
                            photoUrls.add(dataUrl);
                            logger.info("Successfully processed photo: {} - Size: {} bytes, Type: {}, Base64 Length: {}", 
                                       file.getOriginalFilename(), fileBytes.length, contentType, base64Image.length());
                        } else {
                            logger.warn("Malformed data URL for file: {}. Skipping.", file.getOriginalFilename());
                        }
                        
                    } catch (Exception e) {
                        logger.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage());
                        // Continue with other files instead of failing completely
                    }
                }
            }
        }

        logger.info("Processed {} photos successfully out of {} files", photoUrls.size(), 
                    files != null ? files.size() : 0);
        return photoUrls;
    }

    /**
     * Validate if the byte array represents a valid image file
     */
    private boolean isValidImageFile(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) {
            return false;
        }
        
        // Check for common image file signatures
        // JPEG: FF D8 FF
        if (fileBytes.length >= 3 && 
            (fileBytes[0] & 0xFF) == 0xFF && 
            (fileBytes[1] & 0xFF) == 0xD8 && 
            (fileBytes[2] & 0xFF) == 0xFF) {
            return true;
        }
        
        // PNG: 89 50 4E 47
        if (fileBytes.length >= 4 && 
            (fileBytes[0] & 0xFF) == 0x89 && 
            (fileBytes[1] & 0xFF) == 0x50 && 
            (fileBytes[2] & 0xFF) == 0x4E && 
            (fileBytes[3] & 0xFF) == 0x47) {
            return true;
        }
        
        // GIF: 47 49 46 38
        if (fileBytes.length >= 4 && 
            (fileBytes[0] & 0xFF) == 0x47 && 
            (fileBytes[1] & 0xFF) == 0x49 && 
            (fileBytes[2] & 0xFF) == 0x46 && 
            (fileBytes[3] & 0xFF) == 0x38) {
            return true;
        }
        
        // WebP: 52 49 46 46 (RIFF)
        if (fileBytes.length >= 12 && 
            (fileBytes[0] & 0xFF) == 0x52 && 
            (fileBytes[1] & 0xFF) == 0x49 && 
            (fileBytes[2] & 0xFF) == 0x46 && 
            (fileBytes[3] & 0xFF) == 0x46 &&
            (fileBytes[8] & 0xFF) == 0x57 && 
            (fileBytes[9] & 0xFF) == 0x45 && 
            (fileBytes[10] & 0xFF) == 0x42 && 
            (fileBytes[11] & 0xFF) == 0x50) {
            return true;
        }
        
        return false;
    }

    @GetMapping("/{id}/photos")
    public ResponseEntity<List<String>> getTireRequestPhotos(@PathVariable String id) {
        try {
            TireRequest request = tireRequestService.getTireRequestById(id).orElse(null);
            if (request != null) {
                // Use the consolidatePhotos method to ensure consistency
                consolidatePhotos(request);
                
                List<String> photos = request.getTirePhotoUrls() != null ? 
                    new ArrayList<>(request.getTirePhotoUrls()) : new ArrayList<>();
                
                logger.info("Retrieved {} photos for request {} from MongoDB", photos.size(), id);
                
                // Log additional debug info
                if (!photos.isEmpty()) {
                    logger.info("First photo starts with: {}", 
                        photos.get(0).length() > 30 ? photos.get(0).substring(0, 30) + "..." : photos.get(0));
                }
                
                return ResponseEntity.ok(photos);
            } else {
                logger.warn("No tire request found with id: {}", id);
                return ResponseEntity.ok(new ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("Error getting photos for request {}: {}", id, e.getMessage(), e);
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
            logger.debug("Found {} photos in tirePhotoUrls for request {}", request.getTirePhotoUrls().size(), request.getId());
        }
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            // Only add if not already in the list (avoid duplicates)
            for (String photo : request.getPhotoUrls()) {
                if (!allPhotos.contains(photo)) {
                    allPhotos.add(photo);
                }
            }
            logger.debug("Found {} photos in photoUrls for request {}", request.getPhotoUrls().size(), request.getId());
        }
        
        // Update both fields with consolidated photos
        request.setTirePhotoUrls(allPhotos);
        request.setPhotoUrls(allPhotos);
        
        if (!allPhotos.isEmpty()) {
            logger.info("Consolidated {} photos for request {}", allPhotos.size(), request.getId());
            
            // Log first few characters of each photo to verify Base64 data
            for (int i = 0; i < Math.min(allPhotos.size(), 3); i++) {
                String photo = allPhotos.get(i);
                if (photo != null) {
                    String preview = photo.length() > 50 ? photo.substring(0, 50) + "..." : photo;
                    logger.debug("Photo {}: {}", i + 1, preview);
                }
            }
        } else {
            logger.warn("No photos found for request {}", request.getId());
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
