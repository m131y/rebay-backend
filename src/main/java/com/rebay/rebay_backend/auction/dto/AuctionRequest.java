package com.rebay.rebay_backend.auction.dto;

import com.rebay.rebay_backend.Post.entity.SaleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 2200, message = "Content must not exceed 2200 characters")
    private String content;

    @NotNull(message = "Price is required")
    private BigDecimal startPrice;

    private BigDecimal currentPrice;

    @NotNull(message = "start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "end time is required")
    private LocalDateTime endTime;

    private String imageUrl;

    @NotNull(message = "Category is required")
    private int categoryCode;

    private SaleStatus status;
}
