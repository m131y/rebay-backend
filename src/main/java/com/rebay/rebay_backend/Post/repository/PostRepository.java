package com.rebay.rebay_backend.Post.repository;

import com.rebay.rebay_backend.Post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {


    @Query("SELECT p FROM Post p JOIN FETCH p.user ORDER BY p.createdAt DESC")
    Page<Post> findAllWithUser(Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Post> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId ")
    long countByUserId(@Param("userId") Long userId);

    //조회수 관련
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int updateView(@Param("id") Long id);


    @Query("""
    SELECT p FROM Post p
      WHERE p.status = SaleStatus.ON_SALE
      AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
  """)
    Page<Post> findByTitleContains(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
      SELECT p
      FROM Post p
      join p.user u
      WHERE p.status = SaleStatus.ON_SALE
      AND LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Post> findByUsernameContains(@Param("keyword") String keyword, Pageable pageable);

    //입력한 tag만 검색
    @EntityGraph(attributePaths = {"user", "hashtags"})
    @Query("""
       SELECT p FROM Post p
       JOIN p.hashtags h
       WHERE p.status = SaleStatus.ON_SALE
       AND LOWER(h.name) = LOWER(:name)
    """)
    Page<Post> findByHashtagExact(@Param("name") String name, Pageable pageable);

    // 이미 판매 완료되거나, 사용자가 작성한 게시글 제외하고 조회
    @Query("SELECT p FROM Post p " +
            "WHERE p.status <> 'SOLD' AND p.user.id <> :userId")
    List<Post> findRecommendationCandidates(@Param("userId") Long userId);

    @Query("""
    SELECT DISTINCT p.title
    FROM Post p
    WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY p.title ASC
    """)
    Page<String> suggestTitle(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
    SELECT DISTINCT u.username
    FROM Post p
    JOIN p.user u
    WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY u.username ASC
    """)
    Page<String> suggestUsername(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
    SELECT DISTINCT h.name
    FROM Post p
    JOIN p.hashtags h
    WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY h.name ASC
    """)
    Page<String> suggestHashtag(@Param("keyword") String keyword, Pageable pageable);

    List<Post> findByCategoryCode(int categoryCode);
}
