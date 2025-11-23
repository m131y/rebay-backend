package com.rebay.rebay_backend.integratedProduct.dto;

import com.rebay.rebay_backend.Post.dto.HashtagResponse;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.user.dto.UserResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PostResponse와 AuctionResponse가 공통으로 사용하는 필드를 모은 추상 클래스.
 */
@Getter
@Setter
@SuperBuilder
public abstract class ProductCommonResponse {
    private Long id;
    private String title;
    private String content;
    private String thumbnailImageUrl;
    private int categoryCode;
    private SaleStatus status;
    private Integer viewCount;
    private BigDecimal price;
    private BigDecimal currentPrice;
    private UserResponse seller;
    private boolean isLiked;
    private Long likeCount;
    private List<HashtagResponse> hashtags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }