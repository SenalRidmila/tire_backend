package com.example.tire_management.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.tire_management.model.TireOrder;
import com.example.tire_management.repository.TireOrderRepository;

import jakarta.mail.internet.MimeMessage;

@Service
public class TireOrderService {

    @Autowired
    private TireOrderRepository repository;

    @Autowired
    private EmailService emailService;

    @Value("${seller.email}")
    private String sellerEmail;

    public List<TireOrder> getOrdersByVendorEmail(String vendorEmail) {
        return repository.findByVendorEmailIgnoreCase(vendorEmail);
    }

    // Get all orders
    public List<TireOrder> getAllOrders() {
        return repository.findAll();
    }

    // Paginated version for better performance
    public org.springframework.data.domain.Page<TireOrder> getAllOrdersPaginated(
            org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    // Get order by ID
    public Optional<TireOrder> getOrderById(String id) {
        return repository.findById(id);
    }

    // Create new order and send notification email to seller
    public TireOrder createOrder(TireOrder order) {
        order.setVendorEmail(sellerEmail); // hardcoded or from config
        order.setStatus("pending");
        TireOrder savedOrder = repository.save(order);

        // send email to seller
        String link = "http://localhost:3001/seller-dashboard?vendorEmail=" + sellerEmail;
        String message = "Hello,\n\nYou have a new tire order from: " + order.getUserEmail()
                + "\n\nView: " + link + "\n\nThanks!";
        emailService.sendEmail(sellerEmail, "New Tire Order", message);

        return savedOrder;
    }

    // Update existing order
    public TireOrder updateOrder(String id, TireOrder updatedOrder) {
        return repository.findById(id).map(order -> {
            order.setRequestId(updatedOrder.getRequestId());
            order.setVehicleNo(updatedOrder.getVehicleNo());
            order.setVendorName(updatedOrder.getVendorName());
            order.setVendorEmail(updatedOrder.getVendorEmail());
            order.setUserEmail(updatedOrder.getUserEmail());
            order.setQuantity(updatedOrder.getQuantity());
            order.setTireBrand(updatedOrder.getTireBrand());
            order.setLocation(updatedOrder.getLocation());
            order.setStatus(updatedOrder.getStatus());
            order.setRejectionReason(updatedOrder.getRejectionReason());
            return repository.save(order);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    // Delete an order
    public void deleteOrder(String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        repository.deleteById(id);
    }

    // Update order status (from /confirm or /reject)
    public TireOrder updateOrderStatus(String id, String status, String reason) {
        return repository.findById(id).map(order -> {
            order.setStatus(status);
            if ("rejected".equalsIgnoreCase(status)) {
                order.setRejectionReason(reason);
                sendRejectionEmail(order, reason);  // Send rejection email here
            } else if ("confirmed".equalsIgnoreCase(status)) {
                order.setRejectionReason(null);
                sendConfirmationEmail(order);       // Send confirmation email here
            }
            return repository.save(order);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    // Confirm an order and notify user (alternative method)
    public TireOrder confirmOrder(String id) {
        TireOrder order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus("confirmed");
        order.setRejectionReason(null);
        TireOrder savedOrder = repository.save(order);

        // Send confirmation email to user
        sendConfirmationEmail(savedOrder);

        // Optional: Send notification to seller or admin
        sendNotificationToSellerOrAdmin(savedOrder, "confirmed");

        return savedOrder;
    }

    // Reject an order and notify user
    public TireOrder rejectOrder(String id, String reason) {
        TireOrder order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus("rejected");
        order.setRejectionReason(reason);
        TireOrder savedOrder = repository.save(order);

        // Send rejection email to user
        sendRejectionEmail(savedOrder, reason);

        // Optional: Send notification to seller or admin
        sendNotificationToSellerOrAdmin(savedOrder, "rejected");

        return savedOrder;
    }

    // Utility: Send confirmation email
    private void sendConfirmationEmail(TireOrder order) {
        try {
            if (order.getUserEmail() != null && !order.getUserEmail().isBlank()) {
                MimeMessage message = emailService.getMailSender().createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(order.getUserEmail());
                helper.setSubject("Your Tire Order is Confirmed");

                // Email body (you can also use a Thymeleaf template here)
                String body = "Hello,<br><br>Your tire order has been <strong>CONFIRMED</strong>.<br>"
                        + "<ul>"
                        + "<li><b>Vehicle No:</b> " + order.getVehicleNo() + "</li>"
                        + "<li><b>Brand:</b> " + order.getTireBrand() + "</li>"
                        + "<li><b>Quantity:</b> " + order.getQuantity() + "</li>"
                        + "<li><b>Location:</b> " + order.getLocation() + "</li>"
                        + "</ul><br>Details are attached as a PDF.<br><br>Thank you!";
                helper.setText(body, true);

                // Generate PDF
                byte[] pdfBytes = emailService.getPdfGeneratorService().generateTireOrderPDF(order);

                // Attach PDF
                helper.addAttachment("TireOrder_" + order.getId() + ".pdf",
                        new ByteArrayResource(pdfBytes), "application/pdf");

                emailService.getMailSender().send(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendRejectionEmail(TireOrder order, String reason) {
        try {
            if (order.getUserEmail() != null && !order.getUserEmail().isBlank()) {
                MimeMessage message = emailService.getMailSender().createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(order.getUserEmail());
                helper.setSubject("Your Tire Order has been Rejected");

                String body = "Hello,<br><br>Your tire order has been <strong>REJECTED</strong>.<br>"
                        + "<ul>"
                        + "<li><b>Vehicle No:</b> " + order.getVehicleNo() + "</li>"
                        + "<li><b>Brand:</b> " + order.getTireBrand() + "</li>"
                        + "<li><b>Quantity:</b> " + order.getQuantity() + "</li>"
                        + "<li><b>Location:</b> " + order.getLocation() + "</li>"
                        + "<li><b>Rejection Reason:</b> " + reason + "</li>"
                        + "</ul><br>Details are attached as a PDF.<br><br>Thank you!";
                helper.setText(body, true);

                // Generate PDF
                byte[] pdfBytes = emailService.getPdfGeneratorService().generateTireOrderPDF(order);

                // Attach PDF
                helper.addAttachment("TireOrder_" + order.getId() + ".pdf",
                        new ByteArrayResource(pdfBytes), "application/pdf");

                emailService.getMailSender().send(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Optional: Send notification to seller or admin on status change (implement as needed)
    private void sendNotificationToSellerOrAdmin(TireOrder order, String status) {
        // Implement notification logic if needed
    }
}
