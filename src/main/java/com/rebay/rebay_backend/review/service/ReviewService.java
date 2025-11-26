package com.rebay.rebay_backend.review.service;

import com.rebay.rebay_backend.payment.entity.Transaction;
import com.rebay.rebay_backend.payment.repository.TransactionRepository;
import com.rebay.rebay_backend.review.dto.ReviewDto;
import com.rebay.rebay_backend.review.dto.ReviewRequest;
import com.rebay.rebay_backend.review.entity.Review;
import com.rebay.rebay_backend.review.entity.StarRating;
import com.rebay.rebay_backend.review.repository.ReviewRepository;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.exception.ResourceNotFoundException;
import com.rebay.rebay_backend.user.repository.UserRepository;
import com.rebay.rebay_backend.user.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final AuthenticationService authenticationService;

    public ReviewDto createReview(Long transactionId, ReviewRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        Transaction currentTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new ResourceNotFoundException("해당 거래가 없습니다."));
//                .orElseThrow(()-> new ResourceNotFoundException("Transaction not found."));

        if (!currentUser.getId().equals(currentTransaction.getBuyer().getId())) {
            throw new AccessDeniedException("본인이 구매한 상품만 후기를 작성할 수 있습니다.");
//            throw new AccessDeniedException("Only the buyer can write a review for this product.");
        }

        if (!currentTransaction.getIsReceived().equals(Boolean.TRUE)) {
            throw new AccessDeniedException("배송이 완료된 상품만 후기를 작성할 수 있습니다.");
//            throw new AccessDeniedException("Only products with completed delivery can be reviewed.");
        }

        Review review = Review.builder()
                .reviewer(currentUser)
                .transaction(currentTransaction)
                .content(request.getContent())
                .rating(StarRating.of(request.getRating()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ReviewDto.fromEntity(reviewRepository.save(review));
    }

    public ReviewDto updateReview(Long reviewId, ReviewRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        Review currentReview = reviewRepository.findById(reviewId)
                .orElseThrow(()-> new ResourceNotFoundException("해당 리뷰가 없습니다."));
//                .orElseThrow(()-> new ResourceNotFoundException("Review not found."));

        if (!currentReview.getReviewer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("리뷰를 수정할 권한이 없습니다.");
//            throw new AccessDeniedException("You are not authorized to modify this review.");
        }

        if (request.getContent() != null) {
            currentReview.setContent(request.getContent());
        }

        if (request.getRating() != 0) {
            currentReview.setRating(StarRating.of(request.getRating()));
        }

        currentReview.setUpdatedAt(LocalDateTime.now());
        return ReviewDto.fromEntity(reviewRepository.save(currentReview));
    }

    public void deleteReview(Long reviewId) {
        User currentUser = authenticationService.getCurrentUser();

        Review currentReview = reviewRepository.findById(reviewId)
                .orElseThrow(()-> new ResourceNotFoundException("해당 리뷰가 없습니다."));
//                .orElseThrow(()-> new ResourceNotFoundException("Review not found."));

        if (!currentReview.getReviewer().equals(currentUser)) {
            throw new AccessDeniedException("리뷰를 삭제할 권한이 없습니다.");
//            throw new AccessDeniedException("You are not authorized to delete this review.");
        }

        reviewRepository.delete(currentReview);
    }

    public ReviewDto getReview(Long reviewId) {
        Review currentReview = reviewRepository.findById(reviewId)
                .orElseThrow(()-> new ResourceNotFoundException("해당 리뷰가 없습니다."));
//                .orElseThrow(()-> new ResourceNotFoundException("Review not found."));

        return ReviewDto.fromEntity(currentReview);
    }

    public Page<ReviewDto> getReviewerReviews(Long reviewerId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAllByReviewerIdOrderByCreatedAt(reviewerId, pageable);
        return reviews.map(review -> {
            return ReviewDto.fromEntity(review);
        });
    }

    public Page<ReviewDto> getSellerReviews(Long sellerId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAllByTransactionSellerIdOrderByCreatedAt(sellerId, pageable);
        return reviews.map(review -> {
            return ReviewDto.fromEntity(review);
        });
    }

    public Long getReviewsCountByUser(Long userId) {
        return reviewRepository.countByTransactionSellerId(userId);
    }

    public boolean existsReviewByTransactionId(Long transactionId) {
        return reviewRepository.existsByTransactionId(transactionId);
    }
}
