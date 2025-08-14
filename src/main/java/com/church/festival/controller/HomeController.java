package com.church.festival.controller;

import com.church.festival.entity.Artwork;
import com.church.festival.service.ArtworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller for main application pages
 */
@Controller
public class HomeController {

    @Autowired
    private ArtworkService artworkService;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // Get featured/top-rated artworks for homepage
        List<Artwork> featuredArtworks = artworkService.getTopRatedArtworks(6);
        model.addAttribute("featuredArtworks", featuredArtworks);
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get statistics for dashboard
        List<Artwork> topArtworks = artworkService.getTopRatedArtworks(10);
        model.addAttribute("topArtworks", topArtworks);
        
        // Add category statistics
        Artwork.Category[] categories = Artwork.Category.values();
        model.addAttribute("categories", categories);
        
        return "dashboard";
    }
}
