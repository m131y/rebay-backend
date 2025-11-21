package com.rebay.rebay_backend.Post.dto;

import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.ProductCategory;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.user.dto.UserDto;
import com.rebay.rebay_backend.user.dto.UserResponse;
import com.rebay.rebay_backend.user.service.UserService;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private BigDecimal price;
    private String imageUrl;
    private List<String> imageUrls;
    private int categoryCode;
    private SaleStatus status;
    private Integer viewCount;
    private UserResponse user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private  boolean isLiked;
    private Long likeCount;
    private List<HashtagResponse> hashtags;

    public static PostResponse from(Post post, UserResponse userResponse) {

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .price(post.getPrice())
                .imageUrl(post.getImageUrl())
                .imageUrls(post.getImageUrls())
                .categoryCode(post.getCategory().getCode())
                .viewCount(post.getViewCount())
                .status(post.getStatus())
                .hashtags(post.getHashtags().stream()
                        .map(HashtagResponse::from)
                        .collect(Collectors.toList()))
                .user(userResponse)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
