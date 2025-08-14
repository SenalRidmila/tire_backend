package com.example.tire_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.example.tire_management.model.TireRequest;
import com.example.tire_management.model.TireOrder;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

@Configuration
public class MongoConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        try {
            // Create indexes for TireRequest collection
            IndexOperations tireRequestIndexOps = mongoTemplate.indexOps(TireRequest.class);
            
            // Index on status field (most frequently queried)
            try {
                tireRequestIndexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC).named("idx_status"));
            } catch (Exception e) {
                // Index might already exist
                System.out.println("Status index might already exist: " + e.getMessage());
            }
            
            // Index on vehicleNo for quick vehicle searches
            try {
                tireRequestIndexOps.ensureIndex(new Index().on("vehicleNo", Sort.Direction.ASC).named("idx_vehicleNo"));
            } catch (Exception e) {
                System.out.println("VehicleNo index might already exist: " + e.getMessage());
            }
            
            // Compound index for status + email queries (for role-based filtering)
            try {
                tireRequestIndexOps.ensureIndex(new Index()
                        .on("status", Sort.Direction.ASC)
                        .on("email", Sort.Direction.ASC)
                        .named("idx_status_email"));
            } catch (Exception e) {
                System.out.println("Status-email index might already exist: " + e.getMessage());
            }
            
            // Create indexes for TireOrder collection
            IndexOperations tireOrderIndexOps = mongoTemplate.indexOps(TireOrder.class);
            
            // Index on vendorEmail for seller dashboard
            try {
                tireOrderIndexOps.ensureIndex(new Index().on("vendorEmail", Sort.Direction.ASC).named("idx_vendorEmail"));
            } catch (Exception e) {
                System.out.println("VendorEmail index might already exist: " + e.getMessage());
            }
            
            // Index on status for filtering
            try {
                tireOrderIndexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC).named("idx_order_status"));
            } catch (Exception e) {
                System.out.println("Order status index might already exist: " + e.getMessage());
            }
            
            // Index on requestId for linking with tire requests
            try {
                tireOrderIndexOps.ensureIndex(new Index().on("requestId", Sort.Direction.ASC).named("idx_requestId"));
            } catch (Exception e) {
                System.out.println("RequestId index might already exist: " + e.getMessage());
            }
            
            System.out.println("MongoDB indexes setup completed for performance optimization");
        } catch (Exception e) {
            System.err.println("Error setting up MongoDB indexes: " + e.getMessage());
        }
    }
}
