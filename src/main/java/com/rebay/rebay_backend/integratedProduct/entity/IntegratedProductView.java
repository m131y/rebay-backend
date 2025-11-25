package com.rebay.rebay_backend.integratedProduct.entity;

import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.integratedProduct.dto.ProductType;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "integrated_product_feed") // 뷰 이름 지정
@Data
public class IntegratedProductView {

    @EmbeddedId
    private IntegratedProductViewId id;

    private String title;
    private String content;
    private int categoryCode;
    private BigDecimal price;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private SaleStatus status;

    private LocalDateTime createdAt;

    private Long sellerId;
}