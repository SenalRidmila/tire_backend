

package com.example.tire_management.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.tire_management.model.TireRequest;

public interface TireRequestRepository extends MongoRepository<TireRequest, String> {

    List<TireRequest> findByStatusIn(List<String> statuses);
    
    // Paginated queries for better performance
    Page<TireRequest> findByStatusIn(List<String> statuses, Pageable pageable);
    
    // Optimized query to get only essential fields for table display
    @Query(value = "{ 'status': { $in: ?0 } }", fields = "{ 'tirePhotoUrls': 0, 'photoUrls': 0 }")
    List<TireRequest> findByStatusInWithoutPhotos(List<String> statuses);
    
    @Query(value = "{ 'status': { $in: ?0 } }", fields = "{ 'tirePhotoUrls': 0, 'photoUrls': 0 }")
    Page<TireRequest> findByStatusInWithoutPhotos(List<String> statuses, Pageable pageable);
    
    // Sorted queries for better user experience
    List<TireRequest> findByStatusIn(List<String> statuses, Sort sort);
    
    // Count queries for pagination info
    long countByStatusIn(List<String> statuses);
}
