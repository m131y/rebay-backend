package com.rebay.rebay_backend.statistics.service;

import com.rebay.rebay_backend.Post.dto.HashtagResponse;
import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.ProductCategory;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import com.rebay.rebay_backend.Post.repository.PostRepository;
import com.rebay.rebay_backend.payment.entity.Transaction;
import com.rebay.rebay_backend.payment.entity.TransactionStatus;
import com.rebay.rebay_backend.payment.repository.PaymentRepository;
import com.rebay.rebay_backend.payment.repository.TransactionRepository;
import com.rebay.rebay_backend.search.repository.SearchRepository;
import com.rebay.rebay_backend.social.repository.LikeRepository;
import com.rebay.rebay_backend.statistics.dto.RecommendedPostDto;
import com.rebay.rebay_backend.statistics.dto.TradeHistory;
import com.rebay.rebay_backend.user.dto.UserResponse;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.repository.UserRepository;
import com.rebay.rebay_backend.user.service.AuthenticationService;
import com.rebay.rebay_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {
    private static final double W_LIKE = 0.7;    // 좋아요 기반 점수 가중치
    private static final double W_SEARCH = 0.3;  // 검색 기반 점수 가중치
    private static final long TIME_PERIOD_DAYS = 30; // 검색 기록 분석 기간 (30일)
    private static final double W_LEAF = 1.0;    // 소분류
    private static final double W_MID = 0.7;     // 중분류
    private static final double W_TOP = 0.4;     // 대분류

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SearchRepository searchRepository;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final AuthenticationService authenticationService;


    public List<PostResponse> getTopLikedProductsLastWeek() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<Post> posts = likeRepository.findTopLikedPostsLastWeek(oneWeekAgo);
        return posts.stream().map(post -> PostResponse.from(post, UserResponse.builder().build())).toList();
    }


    public Set<String> getDailyTop10Keywords() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return searchRepository.findTop10PopularKeywordsInOneDay(oneDayAgo);
    }


    public BigDecimal getAverageEarningsPerUser() {
        List<Object[]> results = paymentRepository.findTotalSalesAndUniqueUserCount();

        if (results == null || results.isEmpty() || results.get(0) == null) {
            return BigDecimal.ZERO;
        }

        Object[] result = results.get(0);

        BigDecimal totalSales;
        Object sumResult = result[0];
        log.info("sumResult: ",sumResult);

        if (sumResult == null) {
            totalSales = BigDecimal.ZERO;
        } else if (sumResult instanceof BigDecimal) {
            totalSales = (BigDecimal) sumResult;
        } else {
            totalSales = BigDecimal.ZERO;
        }

        Long uniqueUserCount = (Long) result[1];

        if (totalSales.compareTo(BigDecimal.ZERO) == 0 || uniqueUserCount == null || uniqueUserCount == 0) {
            return BigDecimal.ZERO;
        }
        log.info("uniqueUserCount: ",uniqueUserCount);

        BigDecimal divisor = BigDecimal.valueOf(uniqueUserCount);

        return totalSales.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    public List<PostResponse> getPersonalizedRecommendations() {

        User currentUser = authenticationService.getCurrentUser();

        List<Post> posts = postRepository.findRecommendationCandidates(currentUser.getId());
        List<PostResponse> candidates = posts.stream().map(post -> PostResponse.from(post, userService.mapToUserResponse(post.getUser()))).collect(Collectors.toList());

        // 좋아요 기록이 5개 미만인 경우 주간 인기 상품 반환
        if (likeRepository.countByUserId(currentUser.getId()) < 5) {
            return getTopLikedProductsLastWeek();
        }

        Map<Long, Long> likeScores = getAggregatedLikeScores(currentUser.getId());
        Map<String, Double> searchScores = getSearchScores(currentUser.getId());

        log.info("likescores", likeScores);
        log.info("searchscores", searchScores);
        List<RecommendedPostDto> scoredPosts = new ArrayList<>();

        for (PostResponse post : candidates) {
            double sLike = calculateLikeScore(post, likeScores);
            double sSearch = calculateSearchScore(post, searchScores);

            double score = W_LIKE * sLike + W_SEARCH * sSearch;

            System.out.println(post.getId() + " : score("+score+") = sLike(" +sLike+") + sSearch("+sSearch+")");

            if (score > 0) {
                scoredPosts.add(new RecommendedPostDto(post, score));
            }
        }

        return scoredPosts.stream()
                .sorted(Comparator.comparingDouble(RecommendedPostDto::getScore).reversed())
                .limit(20)
                .map(RecommendedPostDto::getPost)
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getAggregatedLikeScores(Long userId) {
        List<Object[]> rawResults = likeRepository.findLikedCategoryScoresAndPostIds(userId);

        Map<Long, Long> aggregatedScores = new HashMap<>();

        for (Object[] result : rawResults) {
            int leafCategoryIdentifier = ((Number) result[0]).intValue();
            Long likeCount = (Long) result[1];

            Set<Long> ancestors = findAncestors(leafCategoryIdentifier);

            for (Long id : ancestors) {
                aggregatedScores.merge(id, likeCount, Long::sum);
            }
        }
        System.out.println(aggregatedScores);
        return aggregatedScores;
    }

    private Set<Long> findAncestors(int identifier) {
        Set<Long> ancestors = new HashSet<>();

        Category current = categoryRepository.findByCode(identifier).orElse(null);

        if (current == null) {
            log.error("Category not found for ID: {}. Check if the category exists in the database.", identifier);
            return ancestors;
        }


        while (current != null) {
            try {
                ancestors.add(current.getId());
            } catch (Exception e) {
                log.error("Error processing category ID {} into Long ancestor key.", current.getId(), e);
            }
            current = current.getParent();
        }
        return ancestors;
    }

    private Map<String, Double> getSearchScores(Long userId) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(TIME_PERIOD_DAYS);
        List<Object[]> results = searchRepository.findRecentKeywordsWithTime(userId, oneMonthAgo);

        Map<String, Double> weightedScores = new HashMap<>();

        for (Object[] result : results) {
            String keyword = (String) result[0];
            Object timeObject = result[1];
            LocalDateTime createdAt;

            if (timeObject instanceof java.sql.Timestamp) {
                createdAt = ((java.sql.Timestamp) timeObject).toLocalDateTime();
            } else if (timeObject instanceof java.time.LocalDateTime) {
                createdAt = (java.time.LocalDateTime) timeObject;
            } else {
                throw new IllegalArgumentException("지원하지 않는 시간 타입입니다: " + timeObject.getClass().getName());
            }

            long daysSinceSearch = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
            double timeWeight = 1.0 - ((double) daysSinceSearch / TIME_PERIOD_DAYS);

            weightedScores.merge(keyword, timeWeight, Double::sum);
        }
        return weightedScores;
    }

    // 좋아요 기반 점수 계산. 카테고리 계층을 순회하며 각 레벨에 따라 가중치를 적용하여 합산합니다.
    private double calculateLikeScore(PostResponse post, Map<Long, Long> likeScores) {
        int postCategoryCode = post.getCategoryCode();
        Category leafCategory = categoryRepository.findByCode(postCategoryCode).orElse(null);

        if (leafCategory == null) {
            log.error("Candidate Post ID {} has invalid Category ID {}. Score 0.0 returned.", post.getId(), leafCategory.getId());
            return 0.0;
        }

        double totalScore = 0.0;
        Category current = leafCategory;
        int maxLevel = 0;

        Category temp = leafCategory;
        while (temp != null) {
            maxLevel++;
            temp = temp.getParent();
        }

        int currentLevel = maxLevel;

        while (current != null) {
            double weight;
            String levelDescription;

            if (currentLevel == maxLevel) {
                weight = W_LEAF;
                levelDescription = "LEAF";
            } else if (currentLevel == maxLevel - 1 && maxLevel > 1) {
                weight = W_MID;
                levelDescription = "MID";
            } else {
                weight = W_TOP;
                levelDescription = "TOP/ROOT";
            }

            double baseScore = likeScores.getOrDefault(current.getId(), 0L).doubleValue();

            double weightedScore = baseScore * weight;
            totalScore += weightedScore;

            current = current.getParent();
            currentLevel--;
        }

        return totalScore;
    }

    // 검색 기반 점수 계산 (예시: 해시태그와 검색 키워드 일치 시 가중치 점수 부여)
    private double calculateSearchScore(PostResponse post, Map<String, Double> searchScores) {
        double score = 0.0;

        for (Map.Entry<String, Double> entry : searchScores.entrySet()) {
            if (post.getTitle().toLowerCase().contains(entry.getKey().toLowerCase())) {
                score += entry.getValue();
            }
        }

        for (HashtagResponse tag : post.getHashtags()) {
            score += searchScores.getOrDefault(tag.getName(), 0.0);
        }
        return score;
    }

    public List<TradeHistory> getTradeHistory(int categoryCode) {
        List<Transaction> transactions = transactionRepository.findByStatusAndPostCategoryCode(TransactionStatus.COMPLETED, categoryCode);
        List<TradeHistory> history = transactions.stream().map((transaction) ->
                TradeHistory.builder()
                        .transactionId(transaction.getId())
                        .purchasedAt(transaction.getCreatedAt())
                        .price(transaction.getPost().getPrice())
                        .build()).toList();

        return history;
    }
}
