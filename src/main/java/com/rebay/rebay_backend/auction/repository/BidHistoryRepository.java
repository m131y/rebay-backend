package com.rebay.rebay_backend.auction.repository;

import com.rebay.rebay_backend.auction.entity.BidHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {
}