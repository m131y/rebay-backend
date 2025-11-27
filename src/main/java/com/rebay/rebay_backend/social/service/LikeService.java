package com.rebay.rebay_backend.social.service;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.repository.PostRepository;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.auction.repository.AuctionRepository;
import com.rebay.rebay_backend.auction.service.AuctionService;
import com.rebay.rebay_backend.social.entity.Like;
import com.rebay.rebay_backend.social.repository.LikeRepository;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.exception.BadRequestException;
import com.rebay.rebay_backend.user.repository.UserRepository;
import com.rebay.rebay_backend.user.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public boolean toggleLike(Long postId) {
        User currentUser = authenticationService.getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException("Post not found"));

        boolean alreadyLiked = likeRepository.existsByUserAndPost(currentUser, post);

        if (alreadyLiked) {
            likeRepository.deleteByUserAndPost(currentUser, post);
            return false;
        } else {
            Like like = Like.builder()
                    .user(currentUser)
                    .post(post)
                    .build();
            likeRepository.save(like);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public Long getLikeCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    @Transactional(readOnly = true)
    public boolean isLikedByCurrentUser(Long postId) {
        User currentUser = authenticationService.getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException("Post not found"));

        return likeRepository.existsByUserAndPost(currentUser, post);
    }

    public boolean toggleAuctionLike(Long auctionId) {
        User currentUser = authenticationService.getCurrentUser();

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BadRequestException("Auction not found"));

        boolean alreadyLiked = likeRepository.existsByUserAndAuction(currentUser, auction);

        if (alreadyLiked) {
            likeRepository.deleteByUserAndAuction(currentUser, auction);
            return false;
        } else {
            Like like = Like.builder()
                    .user(currentUser)
                    .auction(auction)
                    .build();
            likeRepository.save(like);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public Long getAuctionLikeCount(Long auctionId) {
        return likeRepository.countByAuctionId(auctionId);}

    @Transactional(readOnly = true)
    public boolean isLikedAuctionByCurrentUser(Long auctionId) {
        User currentUser = authenticationService.getCurrentUser();

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BadRequestException("Auction not found"));

        return likeRepository.existsByUserAndAuction(currentUser, auction);
    }
}