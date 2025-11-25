package com.rebay.rebay_backend.payment.dto;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.payment.entity.AuctionStatus;
import com.rebay.rebay_backend.payment.entity.Transaction;
import com.rebay.rebay_backend.payment.entity.TransactionStatus;
import com.rebay.rebay_backend.payment.entity.TransactionType;
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
public class TransactionResponse {

    private Long id;
    private Long postId;
    private String productName;
    private BigDecimal amount;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private Boolean isReceived;
    private String clientKey;
    private LocalDateTime receivedAt;
    private TransactionStatus status;
    private AuctionStatus auctionStatus;
    private TransactionType transactionType;
    private String orderId;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction transaction, String orderId, String clientKey) {
        Long postId;
        String productName;
        BigDecimal amount;

        if (transaction.getTransactionType() == TransactionType.AUCTION) {
            // 경매 거래
            Auction auction = transaction.getAuction();
            if (auction == null) {
                throw new IllegalStateException("경매 정보를 찾을 수 없습니다.");
            }
            postId = auction.getId();
            productName = auction.getTitle();
            amount = auction.getCurrentPrice();
        } else {
            // 일반 거래
            Post post = transaction.getPost();
            if (post == null) {
                throw new IllegalStateException("상품 정보를 찾을 수 없습니다.");
            }
            postId = post.getId();
            productName = post.getTitle();
            amount = post.getPrice();
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .postId(postId)
                .productName(productName)
                .amount(amount)
                .buyerId(transaction.getBuyer().getId())
                .buyerName(transaction.getBuyer().getUsername())
                .sellerId(transaction.getSeller().getId())
                .sellerName(transaction.getSeller().getUsername())
                .isReceived(transaction.getIsReceived())
                .clientKey(clientKey)
                .receivedAt(transaction.getReceivedAt())
                .status(transaction.getStatus())
                .auctionStatus(transaction.getAuctionStatus())
                .transactionType(transaction.getTransactionType())
                .orderId(orderId)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
