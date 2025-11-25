package com.rebay.rebay_backend.review.controller;

import com.rebay.rebay_backend.review.dto.ReviewDto;
import com.rebay.rebay_backend.review.dto.ReviewRequest;
import com.rebay.rebay_backend.review.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/{transactionId}")
    public ResponseEntity<ReviewDto> createReview(
            @PathVariable Long transactionId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.createReview(transactionId, request));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> getReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReview(reviewId));
    }

    @GetMapping("/reviewer/{reviewerId}")
    public ResponseEntity<Page<ReviewDto>> getReviewerReviews(
            @PathVariable Long reviewerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(reviewService.getReviewerReviews(reviewerId, pageable));

    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<ReviewDto>> getSellerReviews(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(reviewService.getSellerReviews(sellerId, pageable));
    }

    @GetMapping("/{transactionId}/check")
    public ResponseEntity<Boolean> existsReviewByTransactionId( @PathVariable Long transactionId) {
        return ResponseEntity.ok(reviewService.existsReviewByTransactionId(transactionId));
    }
}
