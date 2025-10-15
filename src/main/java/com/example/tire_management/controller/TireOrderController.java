package com.example.tire_management.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import com.example.tire_management.model.TireOrder;
import com.example.tire_management.repository.TireOrderRepository;
import com.example.tire_management.repository.TireRequestRepository;
import com.example.tire_management.service.TireOrderService;
import com.example.tire_management.service.EmailService;

@RestController
@RequestMapping("/api/tire-orders")
@CrossOrigin(originPatterns = "*")
public class TireOrderController {

    private static final Logger logger = LoggerFactory.getLogger(TireOrderController.class);

    @Autowired
    private TireOrderService tireOrderService;

    @Autowired
    private TireRequestRepository tireRequestRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TireOrderRepository tireOrderRepository;

    // GET: All orders (Admin view)
    @GetMapping
    public ResponseEntity<List<TireOrder>> getAllOrders() {
        return ResponseEntity.ok()
            .cacheControl(org.springframework.http.CacheControl.maxAge(30, java.util.concurrent.TimeUnit.SECONDS))
            .body(tireOrderService.getAllOrders());
    }

    // Fast paginated endpoint for orders
    @GetMapping("/fast")
    public ResponseEntity<Map<String, Object>> getAllOrdersFast(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("id").descending());
            
            org.springframework.data.domain.Page<TireOrder> orderPage = 
                tireOrderService.getAllOrdersPaginated(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", orderPage.getContent());
            response.put("totalElements", orderPage.getTotalElements());
            response.put("totalPages", orderPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            
            return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.maxAge(30, java.util.concurrent.TimeUnit.SECONDS))
                .body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // GET: Orders for a specific seller (vendorEmail) - Seller Dashboard
    @GetMapping("/vendor/{vendorEmail}")
    public ResponseEntity<List<TireOrder>> getOrdersByVendorEmail(@PathVariable String vendorEmail) {
        List<TireOrder> orders = tireOrderService.getOrdersByVendorEmail(vendorEmail);
        return ResponseEntity.ok(orders);
    }

    // GET: Order by ID
    @GetMapping("/{id}")
    public ResponseEntity<TireOrder> getOrderById(@PathVariable String id) {
        return tireOrderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST: Create new order
    @PostMapping
    public ResponseEntity<TireOrder> createOrder(@RequestBody TireOrder order) {
        TireOrder created = tireOrderService.createOrder(order);
        
        // 📧 Send email notification to Seller (Step 5 of workflow - final step)
        try {
            String tireInfo = String.format("%s - Size: %s", 
                created.getTireBrand() != null ? created.getTireBrand() : "Standard Tire",
                created.getLocation() != null ? created.getLocation() : "Standard Size");
            
            emailService.sendSellerTireOrderNotification(
                created.getId(),
                created.getVehicleNo() != null ? created.getVehicleNo() : "Unknown Vehicle",
                tireInfo,
                String.valueOf(created.getQuantity()),
                created.getUserEmail() != null ? created.getUserEmail() : "unknown@sltelecom.lk"
            );
            logger.info("✅ Seller tire order notification email sent for order: {}", created.getId());
        } catch (Exception e) {
            logger.error("❌ Failed to send seller tire order notification email for order: {}", created.getId(), e);
            // Don't fail the order creation if email fails
        }
        
        return ResponseEntity.status(201).body(created);
    }

    // PUT: Update existing order
    @PutMapping("/{id}")
    public ResponseEntity<TireOrder> updateOrder(@PathVariable String id, @RequestBody TireOrder order) {
        Optional<TireOrder> existing = tireOrderService.getOrderById(id);
        if (existing.isPresent()) {
            TireOrder updated = tireOrderService.updateOrder(id, order);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    // DELETE: Delete order by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        Optional<TireOrder> existing = tireOrderService.getOrderById(id);
        if (existing.isPresent()) {
            tireOrderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // PUT: Confirm order (status = confirmed) and notify user
    @PutMapping("/{id}/confirm")
    public ResponseEntity<TireOrder> confirmOrder(@PathVariable String id) {
        TireOrder updatedOrder = tireOrderService.confirmOrder(id);
        return ResponseEntity.ok(updatedOrder);
    }

    // PUT: Reject order (status = rejected) with reason and notify user
    @PutMapping("/{id}/reject")
    public ResponseEntity<TireOrder> rejectOrder(@PathVariable String id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }
        TireOrder updatedOrder = tireOrderService.rejectOrder(id, reason);
        return ResponseEntity.ok(updatedOrder);
    }
}
