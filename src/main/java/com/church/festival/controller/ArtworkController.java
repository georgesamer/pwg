package com.church.festival.controller;

import com.church.festival.entity.Artwork;
import com.church.festival.entity.User;
import com.church.festival.service.ArtworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/artworks")
public class ArtworkController {

    @Autowired
    private ArtworkService artworkService;

    @GetMapping
    public ResponseEntity<List<Artwork>> getAllArtworks() {
        return ResponseEntity.ok(artworkService.getAllArtworks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Artwork> getArtworkById(@PathVariable Long id) {
        Artwork artwork = artworkService.getArtworkById(id);
        return ResponseEntity.ok(artwork);
    }

    // إذا حابب تضيف رفع صورة لاحقًا: غيّر النوع إلى Multipart/form-data و MultipartFile param
    @PostMapping
    public ResponseEntity<Artwork> createArtwork(
            @Valid @RequestBody Artwork artwork,
            @AuthenticationPrincipal User user) {
        // لو عندك logic لربط الـ artwork بالـ user، حطها هنا: e.g. artwork.setOwner(user);
        Artwork created = artworkService.addArtwork(artwork, null);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Artwork> updateArtwork(
            @PathVariable Long id,
            @Valid @RequestBody Artwork artwork) {
        Artwork updated = artworkService.updateArtwork(id, artwork, null);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtwork(@PathVariable Long id) {
        artworkService.deleteArtwork(id);
        return ResponseEntity.noContent().build();
    }
}
