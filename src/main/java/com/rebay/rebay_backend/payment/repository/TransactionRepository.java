package com.rebay.rebay_backend.payment.repository;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.payment.entity.AuctionStatus;
import com.rebay.rebay_backend.payment.entity.Transaction;
import com.rebay.rebay_backend.payment.entity.TransactionStatus;
import com.rebay.rebay_backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 페이징 + EntityGraph
    @EntityGraph(attributePaths = {"post", "buyer", "seller"})
    Page<Transaction> findByBuyerId(Long buyerId, Pageable pageable);

    @EntityGraph(attributePaths = {"post", "buyer", "seller"})
    Page<Transaction> findBySellerId(Long sellerId, Pageable pageable);

    @Query("""
            SELECT t
            FROM Transaction t
            LEFT JOIN FETCH t.post
            LEFT JOIN FETCH t.auction
            LEFT JOIN FETCH t.buyer
            LEFT JOIN FETCH t.seller
            WHERE t.id = :transactionId
            """)
    Optional<Transaction> findById(@Param("transactionId") Long transactionId);

    @Query("SELECT t FROM Transaction t WHERE t.post.id = :postId AND t.buyer.id = :buyerId AND t.status IN ('PAYMENT_PENDING', 'READY')")
    Optional<Transaction> findActiveTransaction(Long postId, Long buyerId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByStatusAndPostCategoryCode(TransactionStatus status, int categoryCode);

    Optional<Transaction> findByAuctionAndBuyerAndAuctionStatus(
            Auction auction,
            User buyer,
            AuctionStatus auctionStatus
    );

    // 특정 경매의 모든 입찰 Transaction 조회
    List<Transaction> findByAuctionAndAuctionStatus(Auction auction, AuctionStatus auctionStatus);

    @EntityGraph(attributePaths = {"buyer", "seller", "post", "auction"})
    Optional<Transaction> findWithDetailsById(Long id);

}
