package com.church.festival.controller;

import com.church.festival.dto.CommentDTO;
import com.church.festival.entity.Comment;
import com.church.festival.entity.User;
import com.church.festival.service.CommentService;
import com.church.festival.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    /**
     * إضافة تعليق جديد
     */
    @PostMapping
    public Comment addComment(@Valid @RequestBody CommentDTO commentDTO, Principal principal) {
        User user = getCurrentUser(principal); // الحصول على المستخدم الحالي
        return commentService.addComment(commentDTO.getArtworkId(), commentDTO.getText(), user);
    }

    /**
     * تعديل تعليق
     */
    @PutMapping("/{commentId}")
    public Comment updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDTO commentDTO,
            Principal principal
    ) {
        User user = getCurrentUser(principal);
        return commentService.updateComment(commentId, commentDTO.getText(), user);
    }

    /**
     * حذف تعليق
     */
    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable Long commentId, Principal principal) {
        User user = getCurrentUser(principal);
        commentService.deleteComment(commentId, user);
    }

    /**
     * جلب التعليقات لعمل فني معين (بترتيب زمني تنازلي)
     */
    @GetMapping("/artwork/{artworkId}")
    public Page<Comment> getCommentsForArtwork(
            @PathVariable Long artworkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return commentService.getCommentsForArtwork(artworkId, pageable);
    }

    /**
     * جلب آخر التعليقات
     */
    @GetMapping("/recent")
    public List<Comment> getRecentComments(@RequestParam(defaultValue = "5") int limit) {
        return commentService.getRecentComments(limit);
    }

    /**
     * جلب تعليقات مستخدم معين (المستخدم الحالي)
     */
    @GetMapping("/my")
    public List<Comment> getUserComments(Principal principal) {
        User user = getCurrentUser(principal);
        return commentService.getUserComments(user);
    }

    /**
     * الحصول على المستخدم الحالي من قاعدة البيانات عبر الاسم في Principal
     */
    private User getCurrentUser(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
