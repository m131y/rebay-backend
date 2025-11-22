package com.rebay.rebay_backend.auction.dto;

import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.user.dto.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionResponse {

    private Long id;
    private UserResponse seller;
    private String title;
    private String content;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer viewCount;
    private String imageUrl;
    private int categoryCode;
    private SaleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AuctionResponse fromEntity(Auction auction, UserResponse userResponse) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .seller(userResponse)
                .title(auction.getTitle())
                .content(auction.getContent())
                .startPrice(auction.getStartPrice())
                .currentPrice(auction.getCurrentPrice())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .viewCount(auction.getViewCount())
                .imageUrl(auction.getImageUrl())
                .categoryCode(auction.getCategory().getCode())
                .status(auction.getStatus())
                .createdAt(auction.getCreatedAt())
                .updatedAt(auction.getUpdatedAt())
                .build();
    }
}
