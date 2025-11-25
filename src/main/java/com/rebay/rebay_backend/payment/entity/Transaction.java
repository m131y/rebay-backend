package com.rebay.rebay_backend.payment.entity;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.review.entity.Review;
import com.rebay.rebay_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "transactions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType; // DEFAULT or AUCTION

    @Enumerated(EnumType.STRING)
    private AuctionStatus auctionStatus;      // BIDDING / WON / LOSE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isReceived = false;    // 거래 완료 여부

    @Column
    private LocalDateTime receivedAt;   // 완료 시각

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PAYMENT_PENDING;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Review review;

    // 헬퍼 메서드 추가..
    public String getItemTitle() {
        if (transactionType == TransactionType.AUCTION && auction != null) {
            return auction.getTitle();
        }
        return post != null ? post.getTitle() : null;
    }

    public void confirmReceipt() {
        this.isReceived = true;
        this.receivedAt = LocalDateTime.now();
        this.status = TransactionStatus.SETTLEMENT_PENDING;
    }

    public boolean isExpired() {
        LocalDateTime expireAt = this.getCreatedAt().plusMinutes(10);
        return LocalDateTime.now().isAfter(expireAt);
    }

    public void updateAuctionStatus(AuctionStatus newStatus) {
        this.auctionStatus = newStatus;
    }

    public void readyPayment() {
        this.status = TransactionStatus.READY;
    }

    public void confirmPayment() {
        this.status = TransactionStatus.PAID;
    }

    public void completeSettlement() { this.status = TransactionStatus.COMPLETED; }

    public void expirePayment() { this.status = TransactionStatus.EXPIRED; }
}
