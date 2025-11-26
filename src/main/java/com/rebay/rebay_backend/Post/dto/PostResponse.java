package com.rebay.rebay_backend.Post.dto;

import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.ProductCategory;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.integratedProduct.dto.ProductCommonResponse;
import com.rebay.rebay_backend.user.dto.UserDto;
import com.rebay.rebay_backend.user.dto.UserResponse;
import com.rebay.rebay_backend.user.service.UserService;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class PostResponse extends ProductCommonResponse {

    private BigDecimal price;
    private List<String> imageUrls;

    public static PostResponse fromEntity(Post post, UserResponse userResponse) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .seller(userResponse)
                .price(post.getPrice())
                .imageUrls(post.getImageUrls())
                .thumbnailImageUrl(post.getImageUrl())
                .categoryCode(post.getCategory().getCode())
                .viewCount(post.getViewCount())
                .status(post.getStatus())
                .hashtags(post.getHashtags().stream()
                        .map(HashtagResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
