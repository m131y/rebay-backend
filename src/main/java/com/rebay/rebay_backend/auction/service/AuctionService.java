package com.rebay.rebay_backend.auction.service;

import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.Post.exception.UnauthorizedException;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import com.rebay.rebay_backend.auction.dto.AuctionRequest;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.auction.repository.AuctionRepository;
import com.rebay.rebay_backend.user.dto.UserResponse;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;

    public AuctionResponse createAuction(AuctionRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        Category currentCategory = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다."));

        Auction auction = Auction.builder()
                .seller(currentUser)
                .title(request.getTitle())
                .content(request.getContent())
                .startPrice(request.getStartPrice())
                .currentPrice(request.getStartPrice())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .imageUrl(request.getImageUrl())
                .category(currentCategory)
                .status(SaleStatus.ON_SALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Auction savedAuction = auctionRepository.save(auction);
        UserResponse userResponse = userService.mapToUserResponse(currentUser);

        return AuctionResponse.fromEntity(savedAuction, userResponse);
    }

    public AuctionResponse getAuction(Long auctionId) {
        User currentUser = authenticationService.getCurrentUser();

        Auction currentAuction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("경매 상품을 찾을 수 없습니다."));

        UserResponse userResponse = userService.mapToUserResponse(currentAuction.getSeller());

        return AuctionResponse.fromEntity(currentAuction, userResponse);
    }

    public Page<AuctionResponse> getAuctions(Pageable pageable) {
        User currentUser = authenticationService.getCurrentUser();

        Page<Auction> auctions = auctionRepository.findAllWithUser(pageable);
        return auctions.map(auction -> {
            UserResponse userResponse = userService.mapToUserResponse(auction.getSeller());
            AuctionResponse response = AuctionResponse.fromEntity(auction,userResponse);
//            Long likeCount = likeRepository.countByPostId(post.getId());
//            boolean isLiked = likeRepository.existsByUserAndPost(currentUser, post);
//
//            response.setLiked(isLiked);
//            response.setLikeCount(likeCount);

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
        Category currentCategory = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다."));

        // 현재 시간이 starttime 이전인지 확인

        currentAuction.setTitle(request.getTitle());
        currentAuction.setContent(request.getContent());
        currentAuction.setStartPrice(request.getStartPrice());
        currentAuction.setCurrentPrice(request.getStartPrice());
        currentAuction.setStartTime(request.getStartTime());
        currentAuction.setEndTime(request.getEndTime());
        currentAuction.setImageUrl(request.getImageUrl());
        currentAuction.setCategory(currentCategory);
        currentAuction.setUpdatedAt(LocalDateTime.now());

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
}
