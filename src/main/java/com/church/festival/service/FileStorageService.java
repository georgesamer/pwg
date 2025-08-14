package com.church.festival.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    private final Cloudinary cloudinary;
    
    public String storeFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }
            
            Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "church-festival"
                )
            );
            
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Failed to store file", e);
        }
    }
    
    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
            );
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary", e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}
