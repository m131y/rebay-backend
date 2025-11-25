package com.rebay.rebay_backend.auction.dto;

import com.rebay.rebay_backend.payment.entity.AuctionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AuctionCloseResponse {
    private AuctionStatus auctionStatus; // WON or LOSE
    private BigDecimal finalPrice;
    private Long winnerId;
    private String winnerName;
    private Long transactionId;
}
