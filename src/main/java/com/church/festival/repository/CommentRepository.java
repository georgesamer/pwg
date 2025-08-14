package com.church.festival.repository;

import com.church.festival.entity.Comment;
import com.church.festival.entity.Artwork;
import com.church.festival.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository interface for Comment entity
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByArtworkOrderByCreatedAtDesc(Artwork artwork);
    Page<Comment> findByArtwork(Artwork artwork, Pageable pageable);
    int countByArtwork(Artwork artwork);
    List<Comment> findByUser(User user);
}
