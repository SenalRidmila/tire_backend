package com.example.tire_management.service;

import com.example.tire_management.model.TireRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class TireRequestValidationService {

    private static final int MAX_VEHICLE_NO_LENGTH = 8;
    private static final int MAX_TIRE_QUANTITY = 50;
    private static final int MAX_TUBE_QUANTITY = 50;
    private static final int MAX_COMMENT_LENGTH = 500;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB in bytes
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    public List<String> validateTireRequest(TireRequest request) {
        List<String> errors = new ArrayList<>();

        // Vehicle Number Validation
        validateVehicleNumber(request.getVehicleNo(), errors);
        
        // User Section Validation
        validateUserSection(request.getUserSection(), errors);
        
        // Replacement Date Validation
        validateReplacementDate(request.getReplacementDate(), request.getVehicleNo(), errors);
        
        // Tire Quantity Validation
        validateTireQuantity(request.getNoOfTires(), errors);
        
        // Tube Quantity Validation
        validateTubeQuantity(request.getNoOfTubes(), errors);
        
        // Cost Center Validation
        validateCostCenter(request.getCostCenter(), errors);
        
        // Officer Service Number Validation
        validateOfficerServiceNumber(request.getOfficerServiceNo(), errors);
        
        // Email Validation
        validateEmail(request.getemail(), errors);
        
        // Comments Validation
        validateComments(request.getComments(), errors);

        return errors;
    }

    private void validateVehicleNumber(String vehicleNo, List<String> errors) {
        if (vehicleNo == null || vehicleNo.trim().isEmpty()) {
            errors.add("Vehicle number is required");
        } else if (vehicleNo.length() > MAX_VEHICLE_NO_LENGTH) {
            errors.add("Vehicle number cannot exceed " + MAX_VEHICLE_NO_LENGTH + " characters");
        }
    }

    private void validateUserSection(String userSection, List<String> errors) {
        if (userSection == null || userSection.trim().isEmpty()) {
            errors.add("User section is required and cannot be empty");
        }
    }

    private void validateReplacementDate(String replacementDate, String vehicleNo, List<String> errors) {
        if (replacementDate == null || replacementDate.trim().isEmpty()) {
            errors.add("Replacement date is required");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date requestedDate = sdf.parse(replacementDate);
            Date currentDate = new Date();
            
            // Check if the date is in the future
            if (requestedDate.after(currentDate)) {
                errors.add("Cannot set replacement date in the future for vehicles already requested during restricted periods");
            }
            
        } catch (ParseException e) {
            errors.add("Invalid replacement date format. Please use yyyy-MM-dd format");
        }
    }

    private void validateTireQuantity(String noOfTires, List<String> errors) {
        if (noOfTires == null || noOfTires.trim().isEmpty()) {
            errors.add("Number of tires is required");
            return;
        }
        
        try {
            int quantity = Integer.parseInt(noOfTires);
            if (quantity < 1) {
                errors.add("Number of tires must be at least 1");
            } else if (quantity > MAX_TIRE_QUANTITY) {
                errors.add("Number of tires cannot exceed " + MAX_TIRE_QUANTITY);
            }
        } catch (NumberFormatException e) {
            errors.add("Number of tires must be a valid number");
        }
    }

    private void validateTubeQuantity(String noOfTubes, List<String> errors) {
        if (noOfTubes == null || noOfTubes.trim().isEmpty()) {
            return; // Tubes are optional
        }
        
        try {
            int quantity = Integer.parseInt(noOfTubes);
            if (quantity < 0) {
                errors.add("Number of tubes cannot be negative");
            } else if (quantity > MAX_TUBE_QUANTITY) {
                errors.add("Number of tubes cannot exceed " + MAX_TUBE_QUANTITY);
            }
        } catch (NumberFormatException e) {
            errors.add("Number of tubes must be a valid number");
        }
    }

    private void validateCostCenter(String costCenter, List<String> errors) {
        if (costCenter == null || costCenter.trim().isEmpty()) {
            errors.add("Cost center should be automatically filled according to registered data");
        }
    }

    private void validateOfficerServiceNumber(String officerServiceNo, List<String> errors) {
        if (officerServiceNo == null || officerServiceNo.trim().isEmpty()) {
            errors.add("Officer service number should be automatically filled according to registered data");
        }
    }

    private void validateEmail(String email, List<String> errors) {
        if (email == null || email.trim().isEmpty()) {
            errors.add("Email should be automatically filled according to registered data");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Please provide a valid email address");
        }
    }

    private void validateComments(String comments, List<String> errors) {
        if (comments != null && comments.length() > MAX_COMMENT_LENGTH) {
            errors.add("Comments cannot exceed " + MAX_COMMENT_LENGTH + " characters");
        }
    }

    public List<String> validateImageFiles(MultipartFile[] files) {
        List<String> errors = new ArrayList<>();
        
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    validateSingleImageFile(file, errors);
                }
            }
        }
        
        return errors;
    }

    private void validateSingleImageFile(MultipartFile file, List<String> errors) {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            errors.add("Image file size must be less than 5MB. Current file: " + 
                      file.getOriginalFilename() + " (" + formatFileSize(file.getSize()) + ")");
        }
        
        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            errors.add("Only image files are allowed. Invalid file: " + file.getOriginalFilename());
        }
        
        // Additional check for valid image formats
        if (contentType != null && 
            !contentType.equals("image/jpeg") && 
            !contentType.equals("image/jpg") && 
            !contentType.equals("image/png") && 
            !contentType.equals("image/gif")) {
            errors.add("Only JPEG, PNG, and GIF image formats are supported. Invalid file: " + 
                      file.getOriginalFilename());
        }
    }

    private String formatFileSize(long size) {
        if (size >= 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else if (size >= 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return size + " bytes";
        }
    }

    public TireRequest autoPopulateFields(TireRequest request) {
        // Auto-populate cost center based on user section
        if (request.getCostCenter() == null || request.getCostCenter().trim().isEmpty()) {
            request.setCostCenter(generateCostCenter(request.getUserSection()));
        }
        
        // Auto-populate officer service number if not provided
        if (request.getOfficerServiceNo() == null || request.getOfficerServiceNo().trim().isEmpty()) {
            request.setOfficerServiceNo(generateOfficerServiceNumber());
        }
        
        // Auto-populate email if not provided
        if (request.getemail() == null || request.getemail().trim().isEmpty()) {
            request.setemail(generateEmail(request.getOfficerServiceNo()));
        }
        
        return request;
    }

    private String generateCostCenter(String userSection) {
        // Mock implementation - in real scenario, this would lookup from database
        Map<String, String> sectionToCostCenter = new HashMap<>();
        sectionToCostCenter.put("IT", "IT-001");
        sectionToCostCenter.put("HR", "HR-001");
        sectionToCostCenter.put("Finance", "FIN-001");
        sectionToCostCenter.put("Operations", "OPS-001");
        
        return sectionToCostCenter.getOrDefault(userSection, "GEN-001");
    }

    private String generateOfficerServiceNumber() {
        // Mock implementation - in real scenario, this would be from authenticated user
        return "SVC-" + System.currentTimeMillis() % 10000;
    }

    private String generateEmail(String officerServiceNo) {
        // Mock implementation - in real scenario, this would lookup from user database
        return officerServiceNo.toLowerCase() + "@company.com";
    }
}