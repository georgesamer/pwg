package com.church.festival.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;

    @NotNull
    private Long artworkId;

    @NotBlank(message = "Comment text is required")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String text;

    private String author;
    private LocalDateTime createdAt;
}
