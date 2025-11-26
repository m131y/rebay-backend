package com.rebay.rebay_backend.auction.repository;

import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.auction.entity.BidHistory;
import com.rebay.rebay_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {

    // 최고가 입찰 조회
    Optional<BidHistory> findFirstByAuctionOrderByBidPriceDesc(Auction auction);

    boolean existsByAuctionAndBidder(Auction auction, User bidder);
}