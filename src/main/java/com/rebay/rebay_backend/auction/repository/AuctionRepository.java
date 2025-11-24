package com.rebay.rebay_backend.auction.repository;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Query("SELECT a FROM Auction a JOIN FETCH a.seller ORDER BY a.createdAt DESC")
    Page<Auction> findAllWithUser(Pageable pageable);

    @Query("SELECT a FROM Auction a JOIN FETCH a.seller ORDER BY a.createdAt DESC")
    List<Auction> findAllWithUser();

    //조회수 관련
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Auction a set a.viewCount = a.viewCount + 1 where a.id = :id")
    int updateView(@Param("id") Long id);

    @Query("SELECT a FROM Auction a JOIN FETCH a.seller WHERE a.seller.id = :sellerId ORDER BY a.createdAt DESC")
    List<Auction> findBySellerId(Long sellerId);

    long countBySellerId(@Param("userId") Long userId);

    // 입찰용 락 조회 메서드
    // PESSIMISTIC_WRITE: 트랜잭션 종료 시까지 다른 요청이 수정하지 못하도록 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(@Param("id") Long id);
}
