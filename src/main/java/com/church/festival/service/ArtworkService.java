package com.church.festival.service;

import com.church.festival.entity.Artwork;
import com.church.festival.repository.ArtworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final String uploadDir = "./uploads/artworks/";

    @Autowired
    public ArtworkService(ArtworkRepository artworkRepository) {
        this.artworkRepository = artworkRepository;

        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory!", e);
        }
    }

    // استدعاء الأعمال المنشورة فقط مع حد أقصى 100 (مثال)
    public List<Artwork> getAllArtworks() {
        return artworkRepository.findByIsPublishedTrue(PageRequest.of(0, 100)).getContent();
    }

    public List<Artwork> getArtworksByCategory(Artwork.Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category must not be null");
        }
        return artworkRepository.findByCategoryAndIsPublishedTrue(category, PageRequest.of(0, 100)).getContent();
    }

    public Artwork getArtworkById(Long id) {
        return artworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artwork not found with ID: " + id));
    }

    public Artwork addArtwork(Artwork artwork, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = saveImageFile(imageFile);
            artwork.setImageUrl(fileName);
        }
        return artworkRepository.save(artwork);
    }

    public Artwork updateArtwork(Long id, Artwork updatedArtwork, MultipartFile imageFile) {
        Artwork existingArtwork = getArtworkById(id);

        existingArtwork.setTitle(updatedArtwork.getTitle());
        existingArtwork.setDescription(updatedArtwork.getDescription());
        existingArtwork.setCategory(updatedArtwork.getCategory());

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = saveImageFile(imageFile);
            existingArtwork.setImageUrl(fileName);
        }

        return artworkRepository.save(existingArtwork);
    }

    public void deleteArtwork(Long id) {
        Artwork artwork = getArtworkById(id);
        artworkRepository.delete(artwork);
    }

    public List<Artwork> getTopRatedArtworks(int limit) {
        return artworkRepository.findTopRatedArtworks(PageRequest.of(0, limit));
    }

    private String saveImageFile(MultipartFile file) {
        try {
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = UUID.randomUUID() + "." + fileExtension;
            Path destinationPath = Paths.get(uploadDir).resolve(uniqueFileName);
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            return uniqueFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image file", e);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1);
        }
        return "";
    }
}
