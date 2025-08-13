package com.example.tire_management.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.example.tire_management.model.TireRequest;
import com.example.tire_management.repository.TireRequestRepository;
// Using iText 7 API
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.kernel.geom.PageSize;

@Service
public class TireRequestService {

    private static final Logger logger = LoggerFactory.getLogger(TireRequestService.class);

    @Autowired
    private TireRequestRepository tireRequestRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${manager.email}")
    private String managerEmail;

    @Value("${tto.email:slttto@gmail.com}")
    private String ttoEmail;

    @Value("${engineer.email}")
    private String engineerEmail;

    // Get requests by multiple statuses (used for Manager/TTO/Engineer dashboards)
    public List<TireRequest> getRequestsByStatuses(List<String> statuses) {
        return tireRequestRepository.findByStatusIn(statuses);
    }


    public String createApprovalEmailTemplate(String requestId, String vehicleNo) {
        String orderLink = "http://localhost:3001/order-tires/" + requestId;

        return "<html>"
                + "<body>"
                + "<h2 style='color: #2e6c80;'>Your Tire Request Has Been Approved</h2>"
                + "<p>Hello,</p>"
                + "<p>Your tire request with ID <strong>" + requestId + "</strong> for vehicle <strong>" + vehicleNo + "</strong> has been approved by the Engineer.</p>"
                + "<p><a href='" + orderLink + "' style='color: #1a73e8;'>👉 Order Tires Now</a></p>"
                + "<p>Thank you for using our service.</p>"
                + "<br/>"
                + "<p>Best regards,<br/>Tire Management Team</p>"
                + "</body>"
                + "</html>";
    }



    // Approve by Engineer
    public void approveByEngineer(String requestId) {
        TireRequest request = tireRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setStatus("ENGINEER_APPROVED");
        tireRequestRepository.save(request);

        // ✅ Call email service here
        if (request.getemail() != null && !request.getemail().isEmpty()) {
            emailService.sendOrderLinkToUser(request);  // <== Fixes the error
        }
    }




    

    // Reject by Engineer
    public void rejectByEngineer(String id) {
        Optional<TireRequest> optionalRequest = tireRequestRepository.findById(id);
        if (optionalRequest.isPresent()) {
            TireRequest request = optionalRequest.get();
            request.setStatus("ENGINEER_REJECTED");
            tireRequestRepository.save(request);
        } else {
            throw new RuntimeException("Request not found");
        }
    }

    // Generate PDF for a TireRequest by ID
    public byte[] generateRequestPDF(String requestId) throws IOException {
        Optional<TireRequest> requestOpt = tireRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            throw new IOException("Request not found");
        }
        TireRequest request = requestOpt.get();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            document.add(new Paragraph("Tire Request Report"));
            document.add(new Paragraph("ID: " + request.getId()));
            document.add(new Paragraph("Vehicle No: " + request.getVehicleNo()));
            document.add(new Paragraph("Vehicle Type: " + request.getVehicleType()));
            document.add(new Paragraph("Vehicle Brand: " + request.getVehicleBrand()));
            document.add(new Paragraph("Vehicle Model: " + request.getVehicleModel()));
            document.add(new Paragraph("User Section: " + request.getUserSection()));
            document.add(new Paragraph("Replacement Date: " + request.getReplacementDate()));
            document.add(new Paragraph("Existing Make: " + request.getExistingMake()));
            document.add(new Paragraph("Tire Size: " + request.getTireSize()));
            document.add(new Paragraph("Number of Tires: " + request.getNoOfTires()));
            document.add(new Paragraph("Number of Tubes: " + request.getNoOfTubes()));
            document.add(new Paragraph("Cost Center: " + request.getCostCenter()));
            document.add(new Paragraph("Present KM: " + request.getPresentKm()));
            document.add(new Paragraph("Previous KM: " + request.getPreviousKm()));
            document.add(new Paragraph("Wear Indicator: " + request.getWearIndicator()));
            document.add(new Paragraph("Wear Pattern: " + request.getWearPattern()));
            document.add(new Paragraph("Officer Service No: " + request.getOfficerServiceNo()));
            document.add(new Paragraph("User Email: " + request.getemail()));
            document.add(new Paragraph("Comments: " + request.getComments()));
            document.add(new Paragraph("Status: " + request.getStatus()));
            if (request.getRejectionReason() != null) {
                document.add(new Paragraph("Rejection Reason: " + request.getRejectionReason()));
            }
            if (request.getTtoApprovalDate() != null) {
                document.add(new Paragraph("TTO Approval Date: " + request.getTtoApprovalDate()));
            }
            if (request.getTtoRejectionDate() != null) {
                document.add(new Paragraph("TTO Rejection Date: " + request.getTtoRejectionDate()));
            }
            if (request.getTtoRejectionReason() != null) {
                document.add(new Paragraph("TTO Rejection Reason: " + request.getTtoRejectionReason()));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Error generating PDF", e);
        }
    }

    // Remaining existing methods...
    public List<TireRequest> getAllTireRequests() {
        return tireRequestRepository.findAll();
    }

    public Optional<TireRequest> getTireRequestById(String id) {
        return tireRequestRepository.findById(id);
    }

    public TireRequest createTireRequest(TireRequest request) {
        TireRequest savedRequest = tireRequestRepository.save(request);
        emailService.sendRequestNotification(savedRequest, managerEmail);
        return savedRequest;
    }

    public TireRequest updateTireRequest(String id, TireRequest request) {
        request.setId(id);
        return tireRequestRepository.save(request);
    }

    public void deleteTireRequest(String id) {
        tireRequestRepository.deleteById(id);
    }

    // Manager Approve
    public TireRequest approveTireRequest(String id) {
        TireRequest request = tireRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus("MANAGER_APPROVED");
        request.setRejectionReason(null);
        TireRequest savedRequest = tireRequestRepository.save(request);
        emailService.sendApprovalNotificationToTTO(savedRequest, ttoEmail);
        return savedRequest;
    }

    // Manager Reject
    public TireRequest rejectTireRequest(String id, String reason) {
        TireRequest request = tireRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus("MANAGER_REJECTED");
        request.setRejectionReason(reason);
        return tireRequestRepository.save(request);
    }

    public TireRequest updateTireRequestStatus(String id, Map<String, Object> updates) {
        TireRequest request = tireRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (updates.containsKey("status")) {
            request.setStatus((String) updates.get("status"));
        }
        if (updates.containsKey("ttoApprovalDate")) {
            request.setTtoApprovalDate((String) updates.get("ttoApprovalDate"));
        }
        if (updates.containsKey("ttoRejectionDate")) {
            request.setTtoRejectionDate((String) updates.get("ttoRejectionDate"));
        }
        if (updates.containsKey("ttoRejectionReason")) {
            request.setTtoRejectionReason((String) updates.get("ttoRejectionReason"));
        }
        return tireRequestRepository.save(request);
    }

    public TireRequest approveTireRequestByTTO(String id) {
        try {
            if (id == null || id.isEmpty()) {
                logger.error("Cannot approve request: Invalid request ID");
                throw new IllegalArgumentException("Request ID cannot be null or empty");
            }
            TireRequest request = tireRequestRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Request not found with ID: {}", id);
                        return new RuntimeException("Request not found");
                    });
            logger.info("Current request status before TTO approval: {}", request.getStatus());
            if (!"APPROVED".equals(request.getStatus()) && !"PENDING".equals(request.getStatus())) {
                logger.warn("Unexpected request status for TTO approval. Current status: {}", request.getStatus());
            }
            request.setStatus("TTO_APPROVED");
            request.setTtoApprovalDate(new Date().toString());
            TireRequest savedRequest = tireRequestRepository.save(request);
            logger.info("Request {} approved successfully by TTO", id);

            try {
                String engineerEmail = System.getProperty("engineer.email",
                        System.getenv().getOrDefault("ENGINEER_EMAIL", "engineerslt38@gmail.com"));
                if (engineerEmail == null || engineerEmail.isEmpty()) {
                    logger.error("Cannot send engineer notification: Engineer email is null or empty");
                } else {
                    engineerEmail = engineerEmail.replace(".com.com", ".com");
                    emailService.sendEngineerNotification(savedRequest, engineerEmail);
                    logger.info("Engineer notification email sent successfully for request {}", id);
                }
            } catch (Exception e) {
                logger.error("Failed to send engineer notification for request {}: {}", id, e.getMessage(), e);
            }

            return savedRequest;
        } catch (Exception e) {
            logger.error("Unexpected error in TTO approval process for request {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    // Employee authentication methods
    public Map<String, Object> findEmployeeByEmailAndPassword(String email, String password) {
        try {
            Query query = new Query(Criteria.where("email").is(email).and("password").is(password));
            Map<String, Object> employee = mongoTemplate.findOne(query, Map.class, "employees");
            return employee;
        } catch (Exception e) {
            logger.error("Error finding employee by email and password: {}", e.getMessage(), e);
            return null;
        }
    }

    public Map<String, Object> findEmployeeByEmail(String email) {
        try {
            Query query = new Query(Criteria.where("email").is(email));
            Map<String, Object> employee = mongoTemplate.findOne(query, Map.class, "employees");
            return employee;
        } catch (Exception e) {
            logger.error("Error finding employee by email: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<Map<String, Object>> getAllEmployees() {
        try {
            Query query = new Query();
            query.limit(10); // Limit to first 10 employees for safety
            List<Map<String, Object>> employees = mongoTemplate.find(query, Map.class, "employees");
            return employees;
        } catch (Exception e) {
            logger.error("Error finding all employees: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public Set<String> getCollectionNames() {
        try {
            return mongoTemplate.getCollectionNames();
        } catch (Exception e) {
            logger.error("Error getting collection names: {}", e.getMessage(), e);
            return Set.of();
        }
    }

    public long getEmployeeCount() {
        try {
            return mongoTemplate.getCollection("employees").countDocuments();
        } catch (Exception e) {
            logger.error("Error getting employee count: {}", e.getMessage(), e);
            return 0;
        }
    }

    public Map<String, Object> getFirstEmployee() {
        try {
            Query query = new Query();
            query.limit(1);
            return mongoTemplate.findOne(query, Map.class, "employees");
        } catch (Exception e) {
            logger.error("Error getting first employee: {}", e.getMessage(), e);
            return null;
        }
    }

}
