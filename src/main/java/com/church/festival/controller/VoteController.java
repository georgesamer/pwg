package com.church.festival.controller;

import com.church.festival.entity.Artwork;
import com.church.festival.entity.User;
import com.church.festival.service.ArtworkService;
import com.church.festival.service.UserService;
import com.church.festival.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/votes")
public class VoteController {

    @Autowired
    private VoteService voteService;

    @Autowired
    private ArtworkService artworkService;

    @Autowired
    private UserService userService;

    @PostMapping("/{artworkId}")
    public String toggleVote(@PathVariable Long artworkId, Authentication auth) {
        try {
            Artwork artwork = artworkService.getArtworkById(artworkId);

            if (auth == null || auth.getName() == null) {
                return "error";
            }

            Optional<User> userOpt = userService.findByUsername(auth.getName());
            if (userOpt.isEmpty()) {
                return "error";
            }

            boolean voted = voteService.toggleVote(userOpt.get(), artwork);
            int newVoteCount = voteService.getVoteCount(artworkId);

            return voted ? "voted:" + newVoteCount : "unvoted:" + newVoteCount;

        } catch (RuntimeException re) {
            return "error";
        } catch (Exception e) {
            return "error";
        }
    }
}
