package com.example.tire_management.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 📁 Files Controller - Alternative endpoint for file serving
 * Provides /files/{filename} endpoint as fallback for /uploads/{filename}
 */
@RestController
@RequestMapping("/files")
@CrossOrigin(originPatterns = "*")
public class FilesController {

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            System.out.println("🔍 FilesController: Serving demo image for: " + filename);
            
            // Serve demo image based on filename
            String demoImage = getDemoImageForFilename(filename);
            Resource resource = new ClassPathResource("static/images/" + demoImage);
            
            if (resource.exists()) {
                System.out.println("✅ FilesController: Found demo image: " + demoImage);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .header("X-Fallback-Image", demoImage)
                        .body(resource);
            } else {
                System.out.println("❌ FilesController: Demo image not found: " + demoImage);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            
        } catch (Exception e) {
            System.err.println("❌ FilesController: Error serving file " + filename + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get demo image filename based on the requested filename
     */
    private String getDemoImageForFilename(String filename) {
        String lowerFilename = filename.toLowerCase();
        
        // Try to match with tire-related keywords
        if (lowerFilename.contains("tire1") || lowerFilename.contains("front")) {
            return "tire1.jpeg";
        } else if (lowerFilename.contains("tire2") || lowerFilename.contains("rear")) {
            return "tire2.jpeg";
        } else if (lowerFilename.contains("tire3") || lowerFilename.contains("side")) {
            return "tire3.jpeg";
        } else {
            // Default fallback - cycle through demo images based on hash
            int hash = Math.abs(filename.hashCode());
            String[] demoImages = {"tire1.jpeg", "tire2.jpeg", "tire3.jpeg"};
            return demoImages[hash % demoImages.length];
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\",\"service\":\"FilesController\",\"demo_images\":\"Available\"}");
    }
}