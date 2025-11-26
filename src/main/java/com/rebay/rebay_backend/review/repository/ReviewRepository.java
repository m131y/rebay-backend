package com.rebay.rebay_backend.review.repository;

import com.rebay.rebay_backend.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findAllByTransactionSellerIdOrderByCreatedAt(Long sellerId, Pageable pageable);
    Page<Review> findAllByReviewerIdOrderByCreatedAt(Long reviewerId, Pageable pageable);
    long countByTransactionSellerId(Long sellerId);
    boolean existsByTransactionId(Long transactionId);
}
