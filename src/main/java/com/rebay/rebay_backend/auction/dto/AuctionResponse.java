package com.rebay.rebay_backend.auction.dto;

import com.rebay.rebay_backend.Post.dto.HashtagResponse;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.integratedProduct.dto.ProductCommonResponse;
import com.rebay.rebay_backend.user.dto.UserResponse;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class AuctionResponse extends ProductCommonResponse {
    private BigDecimal currentPrice;
    private String startTime;
    private String endTime;

    public static AuctionResponse fromEntity(Auction auction, UserResponse userResponse) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .seller(userResponse)
                .title(auction.getTitle())
                .content(auction.getContent())
                .price(auction.getPrice())
                .currentPrice(auction.getCurrentPrice())
                .startTime(auction.getStartTime().toString())
                .endTime(auction.getEndTime().toString())
                .viewCount(auction.getViewCount())
                .thumbnailImageUrl(auction.getImageUrl())
                .categoryCode(auction.getCategory().getCode())
                .status(auction.getStatus())
                .hashtags(auction.getHashtags().stream()
                        .map(HashtagResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(auction.getCreatedAt())
                .updatedAt(auction.getUpdatedAt())
                .build();
    }
}
