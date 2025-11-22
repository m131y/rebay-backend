package com.rebay.rebay_backend.auction.repository;

import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Query("SELECT a FROM Auction a JOIN FETCH a.seller ORDER BY a.createdAt DESC")
    Page<Auction> findAllWithUser(Pageable pageable);
}
