package com.rebay.rebay_backend.review.dto;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.payment.entity.Transaction;
import com.rebay.rebay_backend.payment.entity.TransactionType;
import com.rebay.rebay_backend.review.entity.Review;
import com.rebay.rebay_backend.review.entity.StarRating;
import com.rebay.rebay_backend.user.dto.UserDto;
import com.rebay.rebay_backend.user.entity.User;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewDto {
    private Long id;
    private UserDto reviewer;
    private Long transactionId;
    private String postName;
    private String content;
    private int rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewDto fromEntity(Review review) {
        Transaction t = review.getTransaction();

        String postName;

        if (t.getTransactionType() == TransactionType.AUCTION) {
            // 경매 게시글 제목
            postName = t.getAuction().getTitle();
        } else {
            // 일반 거래 게시글 제목
            postName = t.getPost().getTitle();
        }
        return ReviewDto.builder()
                .id(review.getId())
                .reviewer(UserDto.fromEntity(review.getReviewer()))
                .transactionId(review.getTransaction().getId())
                .postName(postName)
                .content(review.getContent())
                .rating(review.getRating().getValue())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
