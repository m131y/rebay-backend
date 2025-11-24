package com.rebay.rebay_backend.auction.service;

import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.Hashtag;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.Post.exception.UnauthorizedException;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import com.rebay.rebay_backend.Post.repository.HashTagRepository;
import com.rebay.rebay_backend.Post.service.PostService;
import com.rebay.rebay_backend.auction.dto.AuctionRequest;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.auction.repository.AuctionRepository;
import com.rebay.rebay_backend.auction.repository.BidHistoryRepository;
import com.rebay.rebay_backend.auction.entity.BidHistory;
import com.rebay.rebay_backend.notification.SseService;
import com.rebay.rebay_backend.social.repository.LikeRepository;
import com.rebay.rebay_backend.user.dto.UserResponse;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.exception.ResourceNotFoundException;
import com.rebay.rebay_backend.user.repository.UserRepository;
import com.rebay.rebay_backend.user.service.AuthenticationService;
import com.rebay.rebay_backend.user.service.UserService;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final PostService postService;
    private final HashTagRepository hashTagRepository;
    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;
    private final LikeRepository likeRepository;

    private final UserRepository userRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final SseService sseService;

    public AuctionResponse createAuction(AuctionRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        Category currentCategory = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다."));

        List<String> images = postService.sanitizeImages(request.getImageUrls());

        //대표사진
        String cover = (request.getImageUrl() != null && !request.getImageUrl().isBlank())
                ? request.getImageUrl()
                : (!images.isEmpty() ? images.get(0) : null);


        Auction auction = Auction.builder()
                .seller(currentUser)
                .title(request.getTitle())
                .content(request.getContent())
                .price(request.getPrice())
                .currentPrice(request.getPrice())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .imageUrl(cover)
                .imageUrls(images)
                .category(currentCategory)
                .status(SaleStatus.ON_SALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            for (String hashName : request.getHashtags()) {
                Hashtag hashtag = hashTagRepository.findByName(hashName)
                        .orElseGet(() -> hashTagRepository.save(
                                Hashtag.builder()
                                        .name(hashName)
                                        .build()
                        ));
                auction.addHashtag(hashtag);

            }
        }

        Auction savedAuction = auctionRepository.save(auction);
        UserResponse userResponse = userService.mapToUserResponse(currentUser);

        return AuctionResponse.fromEntity(savedAuction, userResponse);
    }

    @Transactional
    public AuctionResponse getAuction(Long auctionId) {
        auctionRepository.updateView(auctionId);

        Auction currentAuction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("경매 상품을 찾을 수 없습니다."));

        UserResponse userResponse = userService.mapToUserResponse(currentAuction.getSeller());

        return AuctionResponse.fromEntity(currentAuction, userResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuctionResponse> getAuctions(Pageable pageable) {
        User currentUser = authenticationService.getCurrentUser();

        Page<Auction> auctions = auctionRepository.findAllWithUser(pageable);
        return auctions.map(auction -> {
            UserResponse userResponse = userService.mapToUserResponse(auction.getSeller());
            AuctionResponse response = AuctionResponse.fromEntity(auction,userResponse);
            Long likeCount = likeRepository.countByAuctionId(auction.getId());
            boolean isLiked = likeRepository.existsByUserAndAuction(currentUser, auction);

            response.setLiked(isLiked);
            response.setLikeCount(likeCount);

            return response;
        });
    }

    @Transactional(readOnly = true)
    public Page<AuctionResponse> getUserAuctions(Long userId, Pageable pageable) {
        User currentUser = authenticationService.getCurrentUser();
        Page<Auction> auctions = auctionRepository.findBySellerId(userId, pageable);
        return auctions.map(auction -> {
            UserResponse userResponse = userService.mapToUserResponse(auction.getSeller());
            AuctionResponse response = AuctionResponse.fromEntity(auction, userResponse);
            Long likeCount = likeRepository.countByAuctionId(auction.getId());
            boolean isLiked = likeRepository.existsByUserAndAuction(currentUser, auction);

            response.setLiked(isLiked);
            response.setLikeCount(likeCount);
            return response;
        });
    }

    public AuctionResponse updateAuction(Long auctionId, AuctionRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        Auction currentAuction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("경매 상품을 찾을 수 없습니다."));

        // 본인이 작성한지 확인
        if (!currentAuction.getSeller().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("본인이 게시한 상품만 수정할 수 있습니다.");
        }

        // 카테고리 유효한지 확인
        Category category = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다."));

        List<String> images = postService.sanitizeImages(request.getImageUrls());
        String cover = (request.getImageUrl() != null && !request.getImageUrl().isBlank())
                ? request.getImageUrl()
                : (!images.isEmpty() ? images.get(0) : null);


        // 현재 시간이 starttime 이전인지 확인

        currentAuction.setTitle(request.getTitle());
        currentAuction.setContent(request.getContent());
        currentAuction.setPrice(request.getPrice());
        currentAuction.setCurrentPrice(request.getPrice());
        currentAuction.setStartTime(request.getStartTime());
        currentAuction.setEndTime(request.getEndTime());
        currentAuction.setImageUrl(cover);
        currentAuction.setImageUrls(images);
        currentAuction.setCategory(category);
        currentAuction.setUpdatedAt(LocalDateTime.now());

        currentAuction.getHashtags().clear();

        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            for (String hashName : request.getHashtags()) {
                Hashtag hashtag = hashTagRepository.findByName(hashName)
                        .orElseGet(() -> hashTagRepository.save(
                                Hashtag.builder()
                                        .name(hashName)
                                        .build()
                        ));
                currentAuction.addHashtag(hashtag);

            }
        }

        Auction savedAuction = auctionRepository.save(currentAuction);
        UserResponse userResponse = userService.mapToUserResponse(currentUser);

        return AuctionResponse.fromEntity(savedAuction, userResponse);
    }

    public void deleteAuction(Long auctionId) {
        User currentUser = authenticationService.getCurrentUser();

        Auction currentAuction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("경매 상품을 찾을 수 없습니다."));

        // 본인이 작성한지 확인
        if (!currentAuction.getSeller().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("본인이 게시한 상품만 삭제할 수 있습니다.");
        }

        auctionRepository.delete(currentAuction);
    }

    public Long getAuctionsCountByUser(Long userId) {
        return auctionRepository.countBySellerId(userId);
    }

    // 입찰 메서드
    @Transactional
    public void placeBid(Long auctionId, BigDecimal bidAmount, Long bidderId) {
        // 비관적 락으로 경매 조회 (동시성 제어)
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("경매를 찾을 수 없습니다."));

        // 유효성 검사
        if (auction.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("이미 종료된 경매입니다.");
        }
        if (auction.getSeller().getId().equals(bidderId)) {
            throw new IllegalArgumentException("판매자는 본인 상품에 입찰할 수 없습니다.");
        }
        if (bidAmount.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new IllegalArgumentException("현재 최고가보다 높은 금액을 제시해야 합니다.");
        }

        // 입찰 내역 저장
        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        BidHistory bidHistory = BidHistory.builder()
                .auction(auction)
                .bidder(bidder)
                .bidPrice(bidAmount)
                .build();

        bidHistoryRepository.save(bidHistory);

        // 경매 현재가 업데이트
        auction.setCurrentPrice(bidAmount);

        // 변경된 가격 정보를 접속한 모든 사람에게 전송 (실시간/SSE)
        Map<String, Object> updateData = Map.of(
                "type", "PRICE_UPDATE",
                "auctionId", auctionId,
                "currentPrice", bidAmount,
                "bidderId", bidderId,
                "bidderName", bidder.getUsername()
        );

        sseService.broadcastToAuction(auctionId, updateData);
    }
}