package com.rebay.rebay_backend.social.repository;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.social.entity.Like;
import com.rebay.rebay_backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.id = :postId")
    Long countByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.auction.id = :auctionId")
    Long countByAuctionId(@Param("auctionId") Long auctionId);

    Long countByUserId(Long userId);

    boolean existsByUserAndPost(User user, Post post);

    boolean existsByUserAndAuction(User user, Auction auction);

    void deleteByUserAndPost(User user, Post post);

    // 일주일 내 좋아요가 가장 많은 Post들을 좋아요 수 기준 내림차순으로 페이지 조회
    @Query(
            value = "SELECT p.* " +
                    "FROM posts p " +
                    "INNER JOIN (" +
                    "    SELECT l.post_id, COUNT(l.post_id) as like_count " +
                    "    FROM likes l " +
                    "    WHERE l.created_at >= :oneWeekAgo " +
                    "    GROUP BY l.post_id " +
                    ") AS weekly_likes " +
                    "ON p.id = weekly_likes.post_id " +
                    "ORDER BY weekly_likes.like_count DESC " +
                    "LIMIT 10",
            nativeQuery = true
    )
    List<Post> findTopLikedPostsLastWeek(@Param("oneWeekAgo") LocalDateTime oneWeekAgo);

    // 특정 유저가 좋아요를 누른 게시글들의 카테고리별 카운트와 Post ID를 조회
    @Query(
            value =  "SELECT p.category_code, COUNT(l.post_id), STRING_AGG(l.post_id::text, ',') " +
                    "FROM likes l " +
                    "JOIN posts p ON l.post_id = p.id " +
                    "WHERE l.user_id = :userId " +
                    "GROUP BY p.category_code " +
                    "ORDER BY COUNT(l.post_id) DESC",
            nativeQuery = true
    )
    List<Object[]> findLikedCategoryScoresAndPostIds(@Param("userId") Long userId);
}