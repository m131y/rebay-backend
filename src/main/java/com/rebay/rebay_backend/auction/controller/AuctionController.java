package com.rebay.rebay_backend.auction.controller;

import com.rebay.rebay_backend.auction.dto.AuctionRequest;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.service.AuctionService;
import com.rebay.rebay_backend.social.entity.Like;
import com.rebay.rebay_backend.social.service.LikeService;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.service.AuthenticationService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auction")
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;
    private final LikeService likeService;

    private final AuthenticationService authenticationService;
    private final com.rebay.rebay_backend.notification.SseService sseService;

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(@Valid @RequestBody AuctionRequest request) {
        return ResponseEntity.ok(auctionService.createAuction(request));
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.getAuction(auctionId));
    }

    @GetMapping
    public ResponseEntity<Page<AuctionResponse>> getAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAuctions(pageable);

        return ResponseEntity.ok(auctions);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuctionResponse>> getUserAuctions(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(auctionService.getUserAuctions(userId));
    }

    @PutMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> updateAuction(
            @PathVariable Long auctionId,
            @Valid @RequestBody AuctionRequest request
    ) {
        return ResponseEntity.ok(auctionService.updateAuction(auctionId, request));
    }

    @DeleteMapping("/{auctionId}")
    public ResponseEntity<Void> deleteAuction(@PathVariable Long auctionId) {
        auctionService.deleteAuction(auctionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{auctionId}/like")
    public ResponseEntity<?> toggleAuctionLike(@PathVariable Long auctionId) {
        boolean isLiked = likeService.toggleAuctionLike(auctionId);
        Long likeCount = likeService.getAuctionLikeCount(auctionId);

        return ResponseEntity.ok().body(Map.of(
                "isLiked", isLiked,
                "likeCount", likeCount)
        );
    }

    @GetMapping("{auctionId}/like")
    public Long getAuctionLikeCount(@PathVariable Long auctionId) {
        return likeService.getAuctionLikeCount(auctionId);
    }

    @GetMapping("{auctionId}/likeCount")
    public boolean isLikedAuctionByCurrentUser(@PathVariable Long auctionId) {
        return likeService.isLikedAuctionByCurrentUser(auctionId);
    }
    // 입찰
    @PostMapping("/{auctionId}/bid")
    public ResponseEntity<Void> placeBid(
            @PathVariable Long auctionId,
            @RequestBody Map<String, BigDecimal> request
    ) {
        User currentUser = authenticationService.getCurrentUser();
        BigDecimal amount = request.get("amount");

        if (amount == null) {
            throw new IllegalArgumentException("입찰 금액(amount)은 필수입니다.");
        }

        auctionService.placeBid(auctionId, amount, currentUser.getId());

        return ResponseEntity.ok().build();
    }

    // 실시간 경매 연결 (SSE Stream)
    @GetMapping(value = "/{auctionId}/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAuction(@PathVariable Long auctionId) {
        return sseService.subscribeAuction(auctionId);
    }

}
