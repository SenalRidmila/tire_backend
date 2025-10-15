package com.example.tire_management.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
@CrossOrigin(originPatterns = "*")
public class FileController {

    private static final String UPLOADS_DIR = "uploads/";

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            System.out.println("🔍 FileController: Attempting to serve file: " + filename);
            
            // First, try to find the file in the uploads directory
            Path filePath = Paths.get(UPLOADS_DIR + filename);
            File file = filePath.toFile();
            
            if (file.exists() && file.isFile()) {
                System.out.println("✅ FileController: Found file locally: " + file.getAbsolutePath());
                
                Resource resource = new FileSystemResource(file);
                String contentType = Files.probeContentType(filePath);
                
                if (contentType == null) {
                    // Determine content type based on file extension
                    String extension = getFileExtension(filename).toLowerCase();
                    switch (extension) {
                        case "jpg":
                        case "jpeg":
                            contentType = "image/jpeg";
                            break;
                        case "png":
                            contentType = "image/png";
                            break;
                        case "gif":
                            contentType = "image/gif";
                            break;
                        case "webp":
                            contentType = "image/webp";
                            break;
                        default:
                            contentType = "application/octet-stream";
                    }
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(resource);
            } else {
                System.out.println("❌ FileController: File not found locally: " + file.getAbsolutePath());
                
                // Return 404 with helpful error message
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }
            
        } catch (Exception e) {
            System.err.println("❌ FileController: Error serving file " + filename + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    
    private String getFileExtension(String filename) {
        int lastIndexOfDot = filename.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return ""; // No extension found
        }
        return filename.substring(lastIndexOfDot + 1);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        File uploadsDir = new File(UPLOADS_DIR);
        boolean exists = uploadsDir.exists();
        int fileCount = exists ? (uploadsDir.listFiles() != null ? uploadsDir.listFiles().length : 0) : 0;
        
        String status = String.format(
            "{\"status\":\"UP\",\"uploadsDir\":\"%s\",\"exists\":%b,\"fileCount\":%d}", 
            uploadsDir.getAbsolutePath(), exists, fileCount
        );
        
        return ResponseEntity.ok(status);
    }
}