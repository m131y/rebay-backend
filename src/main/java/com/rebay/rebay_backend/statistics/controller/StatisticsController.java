package com.rebay.rebay_backend.statistics.controller;

import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.Post.service.PostService;
import com.rebay.rebay_backend.auction.service.AuctionService;
import com.rebay.rebay_backend.integratedProduct.dto.ProductFeedItem;
import com.rebay.rebay_backend.review.service.ReviewService;
import com.rebay.rebay_backend.social.service.FollowService;
import com.rebay.rebay_backend.statistics.dto.TradeHistory;
import com.rebay.rebay_backend.statistics.service.StatisticsService;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final ReviewService reviewService;
    private final PostService postService;
    private final FollowService followService;
    private final StatisticsService statisticsService;
    private final AuthenticationService authenticationService;
    private final AuctionService auctionService;

    @GetMapping("/userProfile/{userId}")
    public ResponseEntity<Map<String,Long>> getStatisticsByUserProfile(@PathVariable Long userId) {
        Map<String,Long> counts = new HashMap<>();
        counts.put("all", postService.getPostsCountByUser(userId) + auctionService.getAuctionsCountByUser(userId));
        counts.put("post", postService.getPostsCountByUser(userId));
        counts.put("auction", auctionService.getAuctionsCountByUser(userId));
        counts.put("review", reviewService.getReviewsCountByUser(userId));
        counts.put("follower", followService.getFollowersCount(userId));
        counts.put("following", followService.getFollowingCount(userId));
        return ResponseEntity.ok(counts);
    }

    // 좋아요 기준 일주일 간 인기상품
    @GetMapping("/popular")
    public ResponseEntity<List<ProductFeedItem>> getTopLikedProductsLastWeek() {
        return ResponseEntity.ok(statisticsService.getTopLikedProductsLastWeek());
    }

    // 인기 검색어 = 검색 기록 많은 거
    @GetMapping("/top10Keyword")
    public ResponseEntity<Set<String>> getDailyTop10Keywords() {
        return ResponseEntity.ok(statisticsService.getDailyTop10Keywords());
    }

    // 사용자 평균 판매수익
    @GetMapping("/averageEarning")
    public ResponseEntity<BigDecimal> getAverageEarningsPerUser() {
        return ResponseEntity.ok(statisticsService.getAverageEarningsPerUser());
    }

    // 사용자 검색어 기록 + 좋아요 기록 기반 게시글 추천 알고리즘
    @GetMapping("/personalRecommend")
    public ResponseEntity<List<ProductFeedItem>> getPersonalizedRecommendations() {
        return ResponseEntity.ok(statisticsService.getPersonalizedRecommendations());
    }

    // 시세
    @GetMapping("/tradeHistory/{categoryCode}")
    public ResponseEntity<List<TradeHistory>> getTradeHistory(@PathVariable int categoryCode) {
        return ResponseEntity.ok(statisticsService.getTradeHistory(categoryCode));
    }

}
