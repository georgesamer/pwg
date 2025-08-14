package com.church.festival.repository;

import com.church.festival.entity.Vote;
import com.church.festival.entity.User;
import com.church.festival.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository interface for Vote entity
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByUserAndArtwork(User user, Artwork artwork);
    void deleteByUserAndArtwork(User user, Artwork artwork);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.artwork.id = :artworkId")
    int countByArtworkId(@Param("artworkId") Long artworkId);

    List<Vote> findByArtwork(Artwork artwork);
    List<Vote> findByUser(User user);
}
