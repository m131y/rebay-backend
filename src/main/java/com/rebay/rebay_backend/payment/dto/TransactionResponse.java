package com.rebay.rebay_backend.payment.dto;

import com.rebay.rebay_backend.payment.entity.AuctionStatus;
import com.rebay.rebay_backend.payment.entity.Transaction;
import com.rebay.rebay_backend.payment.entity.TransactionStatus;
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
    private String orderId;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction transaction, String orderId, String clientKey) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .postId(transaction.getPost().getId())
                .productName(transaction.getPost().getTitle())
                .amount(transaction.getPost().getPrice())
                .buyerId(transaction.getBuyer().getId())
                .buyerName(transaction.getBuyer().getUsername())
                .sellerId(transaction.getSeller().getId())
                .sellerName(transaction.getSeller().getUsername())
                .isReceived(transaction.getIsReceived())
                .clientKey(clientKey)
                .receivedAt(transaction.getReceivedAt())
                .status(transaction.getStatus())
                .auctionStatus(transaction.getAuctionStatus())
                .orderId(orderId)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
