package com.example.tire_management.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.tire_management.model.TireOrder;


public interface TireOrderRepository extends MongoRepository<TireOrder, String> {
    List<TireOrder> findByVendorEmailIgnoreCase(String vendorEmail);

}
