package com.church.festival.service;

import com.church.festival.entity.Artwork;
import com.church.festival.entity.User;
import com.church.festival.entity.Vote;
import com.church.festival.repository.VoteRepository;
import com.church.festival.repository.ArtworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VoteService {

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    public Vote castVote(Long artworkId, User voter) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));

        if (voteRepository.existsByUserAndArtwork(voter, artwork)) {
            throw new RuntimeException("User has already voted for this artwork");
        }

        Vote vote = new Vote();
        vote.setArtwork(artwork);
        vote.setUser(voter);

        return voteRepository.save(vote);
    }

    public void removeVote(Long artworkId, User voter) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));

        if (!voteRepository.existsByUserAndArtwork(voter, artwork)) {
            throw new RuntimeException("Vote not found");
        }

        voteRepository.deleteByUserAndArtwork(voter, artwork);
    }

    public List<Vote> getVotesByArtwork(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));
        return voteRepository.findByArtwork(artwork);
    }

    public List<Vote> getVotesByUser(User voter) {
        return voteRepository.findByUser(voter);
    }

    public boolean hasUserVoted(Long artworkId, User voter) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));
        return voteRepository.existsByUserAndArtwork(voter, artwork);
    }

    public int getVoteCount(Long artworkId) {
        // استخدام عدّ مباشر للأصوات بدلاً من الحقل المشتق
        return voteRepository.countByArtworkId(artworkId);
    }

    public boolean toggleVote(User user, Artwork artwork) {
        boolean exists = voteRepository.existsByUserAndArtwork(user, artwork);

        if (exists) {
            voteRepository.deleteByUserAndArtwork(user, artwork);
            return false;
        } else {
            Vote vote = new Vote();
            vote.setArtwork(artwork);
            vote.setUser(user);
            voteRepository.save(vote);
            return true;
        }
    }
}
