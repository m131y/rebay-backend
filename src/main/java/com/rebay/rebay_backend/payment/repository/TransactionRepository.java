package com.rebay.rebay_backend.payment.repository;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.payment.entity.Transaction;
import com.rebay.rebay_backend.payment.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t JOIN FETCH t.post JOIN FETCH t.buyer JOIN FETCH t.seller WHERE t.buyer.id = :buyerId")
    List<Transaction> findByBuyerId(@Param("buyerId") Long buyerId);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.post JOIN FETCH t.buyer JOIN FETCH t.seller WHERE t.seller.id = :sellerId")
    List<Transaction> findBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.post JOIN FETCH t.buyer JOIN FETCH t.seller WHERE t.id = :transactionId")
    Optional<Transaction> findById(@Param("transactionId") Long transactionId);

    @Query("SELECT t FROM Transaction t WHERE t.post.id = :postId AND t.buyer.id = :buyerId AND t.status IN ('PAYMENT_PENDING', 'READY')")
    Optional<Transaction> findActiveTransaction(Long postId, Long buyerId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByStatusAndPostCategoryCode(TransactionStatus status, int categoryCode);

    //    List<Post> findByStatus(SaleStatus status);
}
