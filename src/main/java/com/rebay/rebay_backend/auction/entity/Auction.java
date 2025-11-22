package com.rebay.rebay_backend.auction.entity;

import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Auction {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "start_price", nullable = false)
    private BigDecimal startPrice;

    @Column(name = "current_price", nullable = false)
    private BigDecimal currentPrice;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Builder.Default
    @Column(columnDefinition = "integer default 0", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code", referencedColumnName = "code", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    private SaleStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
