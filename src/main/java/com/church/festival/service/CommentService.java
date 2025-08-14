package com.church.festival.service;

import com.church.festival.dto.CommentDTO;
import com.church.festival.entity.Artwork;
import com.church.festival.entity.Comment;
import com.church.festival.entity.User;
import com.church.festival.repository.ArtworkRepository;
import com.church.festival.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    // ... باقي الميثودات القديمة

    public List<CommentDTO> getCommentsByArtwork(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));

        return commentRepository.findByArtworkOrderByCreatedAtDesc(artwork)
                .stream()
                .map(comment -> new CommentDTO(
                        comment.getId(),
                        comment.getArtwork().getId(),
                        comment.getContent(),
                        comment.getUser().getUsername(),
                        comment.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // المطلوب من الكنترولر: إنشاء تعليق بإرجاع كيان Comment
    public Comment addComment(Long artworkId, String text, User user) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));
        Comment comment = new Comment();
        comment.setArtwork(artwork);
        comment.setUser(user);
        comment.setContent(text);
        return commentRepository.save(comment);
    }

    // تحديث تعليق
    public Comment updateComment(Long commentId, String text, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        // تحقق بسيط على ملكية التعليق (نفس المستخدم)
        if (comment.getUser() == null || !comment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to update this comment");
        }
        comment.setContent(text);
        return commentRepository.save(comment);
    }

    // حذف تعليق
    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (comment.getUser() == null || !comment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to delete this comment");
        }
        commentRepository.delete(comment);
    }

    // جلب التعليقات لعمل فني معين بترتيب تنازلي وتجزئة
    public Page<Comment> getCommentsForArtwork(Long artworkId, Pageable pageable) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));
        // تأكد من ترتيب تنازلي حسب createdAt إن لم يكن pageable يحتوي sort
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return commentRepository.findByArtwork(artwork, pageable);
    }

    // جلب أحدث التعليقات بعدد limit
    public List<Comment> getRecentComments(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentRepository.findAll(pageable).getContent();
    }

    // جلب تعليقات مستخدم معين
    public List<Comment> getUserComments(User user) {
        return commentRepository.findByUser(user);
    }

    // النسخة السابقة التي تعيد DTOs (تبقى للاستخدامات الأخرى)
    public CommentDTO addComment(String content, User commenter, Artwork artwork) {
        Comment comment = new Comment();
        comment.setArtwork(artwork);
        comment.setUser(commenter);
        comment.setContent(content);

        Comment savedComment = commentRepository.save(comment);

        return new CommentDTO(
                savedComment.getId(),
                savedComment.getArtwork().getId(),
                savedComment.getContent(),
                savedComment.getUser().getUsername(),
                savedComment.getCreatedAt()
        );
    }
}
