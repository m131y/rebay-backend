package com.rebay.rebay_backend.auction.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.Hashtag;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.social.entity.Like;
import com.rebay.rebay_backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** 🔹 다중 이미지(첫 번째가 대표) */
    @ElementCollection
    @CollectionTable(
            name = "auction_images",
            joinColumns = @JoinColumn(name = "auction_id")
    )
    @Column(name = "image_url", columnDefinition = "TEXT")
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

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

    @OneToMany(mappedBy = "auction", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<BidHistory> bidHistories = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "auction", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Like> likes = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "auction_hashtag",
            joinColumns = @JoinColumn(name = "auction_id"),
            inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @Builder.Default
    private Set<Hashtag> hashtags = new HashSet<>();

    public void addHashtag(Hashtag hashtag) {
        this.hashtags.add(hashtag);
        hashtag.getAuctions().add(this);
    }

    public void removeHashtag(Hashtag hashtag) {
        this.hashtags.remove(hashtag);
        hashtag.getPosts().remove(this);
    }

    public boolean isInCategory(int targetCode) {
        if (this.category == null) {
            return false;
        }

        Category current = this.category;
        while (current != null) {
            // 현재 카테고리 또는 부모 카테고리의 코드를 확인
            if (current.getCode() == targetCode) {
                return true;
            }
            // 다음 상위 레벨로 이동
            current = current.getParent();
        }
        return false;
    }
}
