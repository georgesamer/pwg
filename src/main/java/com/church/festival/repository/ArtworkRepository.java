package com.church.festival.repository;

import com.church.festival.entity.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository interface for Artwork entity
 */
@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    
    Page<Artwork> findByIsPublishedTrue(Pageable pageable);
    
    Page<Artwork> findByCategoryAndIsPublishedTrue(Artwork.Category category, Pageable pageable);
    
    // ترتيب الأعمال المنشورة حسب عدد الأصوات الفعلي (دون الاعتماد على حقل مشتق غير موجود)
    @Query("SELECT a FROM Artwork a WHERE a.isPublished = true ORDER BY (SELECT COUNT(v) FROM Vote v WHERE v.artwork = a) DESC")
    List<Artwork> findTopRatedArtworks(Pageable pageable);
    
    @Query("SELECT a FROM Artwork a WHERE a.isPublished = true AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))" )
    Page<Artwork> searchArtworks(@Param("keyword") String keyword, Pageable pageable);
}
