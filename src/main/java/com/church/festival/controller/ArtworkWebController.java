package com.church.festival.controller;

import com.church.festival.entity.Artwork;
import com.church.festival.entity.User;
import com.church.festival.service.ArtworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Web Controller for artwork-related pages
 */
@Controller
@RequestMapping("/artworks")
public class ArtworkWebController {

    @Autowired
    private ArtworkService artworkService;

    @GetMapping
    public String artworksPage(Model model) {
        List<Artwork> artworks = artworkService.getAllArtworks();
        model.addAttribute("artworks", artworks);
        
        // Add category statistics
        Artwork.Category[] categories = Artwork.Category.values();
        model.addAttribute("categories", categories);
        
        return "artworks";
    }

    @GetMapping("/view/{id}")
    public String viewArtwork(@PathVariable Long id, Model model) {
        try {
            Artwork artwork = artworkService.getArtworkById(id);
            model.addAttribute("artwork", artwork);
            return "artwork-detail";
        } catch (RuntimeException e) {
            model.addAttribute("error", "Artwork not found");
            return "redirect:/artworks";
        }
    }

    @GetMapping("/upload")
    public String uploadPage(Model model) {
        model.addAttribute("artwork", new Artwork());
        model.addAttribute("categories", Artwork.Category.values());
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadArtwork(@ModelAttribute Artwork artwork,
                               @RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        try {
            Artwork savedArtwork = artworkService.addArtwork(artwork, file);
            redirectAttributes.addFlashAttribute("success", "Artwork uploaded successfully!");
            return "redirect:/artworks/view/" + savedArtwork.getId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload artwork: " + e.getMessage());
            return "redirect:/artworks/upload";
        }
    }

    @GetMapping("/category/{category}")
    public String artworksByCategory(@PathVariable String category, Model model) {
        try {
            Artwork.Category categoryEnum = Artwork.Category.valueOf(category.toUpperCase());
            List<Artwork> artworks = artworkService.getArtworksByCategory(categoryEnum);
            model.addAttribute("artworks", artworks);
            model.addAttribute("selectedCategory", categoryEnum);
            model.addAttribute("categories", Artwork.Category.values());
            return "artworks";
        } catch (IllegalArgumentException e) {
            return "redirect:/artworks";
        }
    }
}