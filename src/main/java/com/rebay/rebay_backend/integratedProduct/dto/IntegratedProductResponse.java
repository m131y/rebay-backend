package com.rebay.rebay_backend.integratedProduct.dto;

import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedProductResponse {

    private ProductType productType;
    private ProductCommonResponse productData;

    // 팩토리 메서드
    public static IntegratedProductResponse from(PostResponse response) {
        IntegratedProductResponse integrated = new IntegratedProductResponse();
        integrated.productType = ProductType.POST;
        integrated.productData = response;
        return integrated;
    }

    public static IntegratedProductResponse from(AuctionResponse response) {
        IntegratedProductResponse integrated = new IntegratedProductResponse();
        integrated.productType = ProductType.AUCTION;
        integrated.productData = response;
        return integrated;
    }

    public LocalDateTime getCreatedAt() {
        // productData는 ProductCommonResponse 타입이지만,
        // 실제로는 PostResponse나 AuctionResponse이므로
        // 이 객체들에 getCreatedAt()이 있다고 가정하고 호출합니다.
        return this.productData.getCreatedAt();
    }

    public ProductFeedItem toProductFeedItem() {
        return ProductFeedItem.builder()
                .productId(this.productData.getId())
                .productType(this.productType)
                .title(this.productData.getTitle())
                .content(this.productData.getContent())
                .price(this.productData.getPrice())
                .currentPrice(this.productData.getCurrentPrice())
                .thumbnailImageUrl(this.productData.getThumbnailImageUrl())
                .status(this.productData.getStatus())
                .createdAt(this.productData.getCreatedAt())
                .categoryCode(this.productData.getCategoryCode())
                .build();
    }
}