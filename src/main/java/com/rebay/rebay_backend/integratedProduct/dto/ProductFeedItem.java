package com.rebay.rebay_backend.integratedProduct.dto;

import com.rebay.rebay_backend.Post.entity.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductFeedItem {
    private Long productId;
    private ProductType productType;
    private String title;
    private String content;
    private int categoryCode;
    private BigDecimal price;
    private BigDecimal currentPrice;
    private String thumbnailImageUrl;
    private SaleStatus status;
    private LocalDateTime createdAt;
}