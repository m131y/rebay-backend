package com.rebay.rebay_backend.auction.repository;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Query("SELECT a FROM Auction a JOIN FETCH a.seller ORDER BY a.createdAt DESC")
    Page<Auction> findAllWithUser(Pageable pageable);

    //조회수 관련
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Auction a set a.viewCount = a.viewCount + 1 where a.id = :id")
    int updateView(@Param("id") Long id);

    Page<Auction> findBySellerId(Long sellerId, Pageable pageable);

    long countBySellerId(@Param("userId") Long userId);
}
