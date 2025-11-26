package com.rebay.rebay_backend.auction.repository;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    // 이미 판매 완료되거나, 사용자가 작성한 게시글 제외하고 조회
    @Query("SELECT a FROM Auction a " +
            "WHERE a.status <> 'SOLD' AND a.seller.id <> :userId")
    List<Auction> findRecommendationCandidates(@Param("userId") Long userId);


    @Query("""
    SELECT a FROM Auction a
      WHERE a.status = SaleStatus.ON_SALE
      AND LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
  """)
    List<Auction> findByTitleContains(@Param("keyword") String keyword);

    @Query("""
      SELECT a
      FROM Auction a
      join a.seller u
      WHERE a.status = SaleStatus.ON_SALE
      AND LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Auction> findByUsernameContains(@Param("keyword") String keyword);

    //입력한 tag만 검색
    @EntityGraph(attributePaths = {"user", "hashtags"})
    @Query("""
       SELECT a FROM Auction a
       JOIN a.hashtags h
       WHERE a.status = SaleStatus.ON_SALE
       AND LOWER(h.name) = LOWER(:name)
    """)
    List<Auction> findByHashtagExact(@Param("name") String name);


    @Query("""
    SELECT DISTINCT a.title
    FROM Auction a
    WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY a.title ASC
    """)
    Page<String> suggestTitle(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
    SELECT DISTINCT u.username
    FROM Auction a
    JOIN a.seller u
    WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY u.username ASC
    """)
    Page<String> suggestUsername(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
    SELECT DISTINCT h.name
    FROM Auction a
    JOIN a.hashtags h
    WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY h.name ASC
    """)
    Page<String> suggestHashtag(@Param("keyword") String keyword, Pageable pageable);
}
