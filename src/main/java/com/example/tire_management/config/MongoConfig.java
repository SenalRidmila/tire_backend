package com.example.tire_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.example.tire_management.model.TireRequest;
import com.example.tire_management.model.TireOrder;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class MongoConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        try {
            System.out.println("🗂️ Checking and setting up MongoDB indexes...");
            
            // Create indexes for TireRequest collection
            IndexOperations tireRequestIndexOps = mongoTemplate.indexOps(TireRequest.class);
            
            // Check if indexes exist and create only if needed
            List<IndexInfo> existingIndexes = tireRequestIndexOps.getIndexInfo();
            Set<String> existingIndexNames = existingIndexes.stream()
                    .map(IndexInfo::getName)
                    .collect(Collectors.toSet());
            
            // Index on status field (most frequently queried)
            if (!hasIndexOnField(existingIndexes, "status")) {
                try {
                    tireRequestIndexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC).named("idx_status"));
                    System.out.println("✅ Created status index for TireRequest");
                } catch (Exception e) {
                    System.out.println("ℹ️ Status index already exists with different configuration");
                }
            } else {
                System.out.println("ℹ️ Status index already exists for TireRequest");
            }
            
            // Index on vehicleNo for quick vehicle searches
            if (!hasIndexOnField(existingIndexes, "vehicleNo")) {
                try {
                    tireRequestIndexOps.ensureIndex(new Index().on("vehicleNo", Sort.Direction.ASC).named("idx_vehicleNo"));
                    System.out.println("✅ Created vehicleNo index for TireRequest");
                } catch (Exception e) {
                    System.out.println("ℹ️ VehicleNo index already exists with different configuration");
                }
            } else {
                System.out.println("ℹ️ VehicleNo index already exists for TireRequest");
            }
            
            // Create indexes for TireOrder collection
            IndexOperations tireOrderIndexOps = mongoTemplate.indexOps(TireOrder.class);
            List<IndexInfo> orderIndexes = tireOrderIndexOps.getIndexInfo();
            
            // Index on vendorEmail for seller dashboard
            if (!hasIndexOnField(orderIndexes, "vendorEmail")) {
                try {
                    tireOrderIndexOps.ensureIndex(new Index().on("vendorEmail", Sort.Direction.ASC).named("idx_vendorEmail"));
                    System.out.println("✅ Created vendorEmail index for TireOrder");
                } catch (Exception e) {
                    System.out.println("ℹ️ VendorEmail index already exists with different configuration");
                }
            } else {
                System.out.println("ℹ️ VendorEmail index already exists for TireOrder");
            }
            
            // Index on requestId for linking with tire requests
            if (!hasIndexOnField(orderIndexes, "requestId")) {
                try {
                    tireOrderIndexOps.ensureIndex(new Index().on("requestId", Sort.Direction.ASC).named("idx_requestId"));
                    System.out.println("✅ Created requestId index for TireOrder");
                } catch (Exception e) {
                    System.out.println("ℹ️ RequestId index already exists with different configuration");
                }
            } else {
                System.out.println("ℹ️ RequestId index already exists for TireOrder");
            }
            
            System.out.println("🗂️ MongoDB indexes setup completed successfully");
        } catch (Exception e) {
            System.err.println("Error setting up MongoDB indexes: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to check if an index exists on a specific field
     */
    private boolean hasIndexOnField(List<IndexInfo> indexes, String fieldName) {
        return indexes.stream()
                .anyMatch(indexInfo -> indexInfo.getIndexFields().stream()
                        .anyMatch(field -> field.getKey().equals(fieldName)));
    }
}
