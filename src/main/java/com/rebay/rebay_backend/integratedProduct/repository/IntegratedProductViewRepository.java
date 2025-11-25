package com.rebay.rebay_backend.integratedProduct.repository;

import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.integratedProduct.dto.ProductType;
import com.rebay.rebay_backend.integratedProduct.entity.IntegratedProductView;
import com.rebay.rebay_backend.integratedProduct.entity.IntegratedProductViewId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface IntegratedProductViewRepository extends JpaRepository<IntegratedProductView, IntegratedProductViewId> {
    Page<IntegratedProductView> findByCategoryCodeAndStatus(
            int categoryCode,
            SaleStatus status,
            Pageable pageable
    );

    Page<IntegratedProductView> findByCategoryCodeInAndStatusIn(
            Collection<Integer> categoryCodes,
            Collection<SaleStatus> statuses,
            Pageable pageable
    );

    Page<IntegratedProductView> findByIdProductTypeAndStatusIn(
            ProductType productType,
            Collection<SaleStatus> statuses,
            Pageable pageable
    );

    Page<IntegratedProductView> findByIdProductTypeAndStatus(
            ProductType productType,
            SaleStatus status,
            Pageable pageable
    );

    Page<IntegratedProductView> findByStatus(
            SaleStatus status,
            Pageable pageable
    );

    Page<IntegratedProductView> findByStatusIn(
            Collection<SaleStatus> statuses,
            Pageable pageable
    );

    Page<IntegratedProductView> findByIdProductTypeAndCategoryCodeInAndStatusIn(
            ProductType productType,
            Collection<Integer> categoryCodes,
            Collection<SaleStatus> requiredStatuses,
            Pageable pageable
    );

    // 이미 판매 완료되거나, 사용자가 작성한 게시글 제외하고 조회
    @Query(
            "SELECT ipf " +
                    "FROM IntegratedProductView ipf " +
                    "WHERE ipf.status <> 'SOLD' " +
                    "  AND ipf.sellerId <> :userId " +
                    "ORDER BY ipf.createdAt DESC"
    )
    List<IntegratedProductView> findRecommendationCandidates(@Param("userId") Long userId);

    List<IntegratedProductView> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Query("""
      SELECT ipf 
      FROM IntegratedProductView ipf
      WHERE ipf.status = SaleStatus.ON_SALE
      AND LOWER(ipf.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<IntegratedProductView> findByTitleContains(@Param("keyword") String keyword);

    @Query("""
      SELECT ipf
      FROM IntegratedProductView ipf
      join ipf.seller u
      WHERE ipf.status = SaleStatus.ON_SALE
      AND LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<IntegratedProductView> findByUsernameContains(@Param("keyword") String keyword);

//    //입력한 tag만 검색
//    @EntityGraph(attributePaths = {"user", "hashtags"})
//    @Query("""
//       SELECT ipf
//       FROM IntegratedProductView ipf
//       JOIN ipf.hashtags h
//       WHERE ipf.status = SaleStatus.ON_SALE
//       AND LOWER(h.name) = LOWER(:name)
//    """)
//    List<IntegratedProductView> findByHashtagExact(@Param("name") String name);

    @Query("""
    SELECT DISTINCT ipf.title
    FROM IntegratedProductView ipf
    WHERE LOWER(ipf.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY ipf.title ASC
    """)
    Page<String> suggestTitle(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
    SELECT DISTINCT u.username
    FROM IntegratedProductView ipf
    JOIN ipf.seller u
    WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY u.username ASC
    """)
    Page<String> suggestUsername(@Param("keyword") String keyword, Pageable pageable);

//    @Query("""
//    SELECT DISTINCT h.name
//    FROM IntegratedProductView ipf
//    JOIN ipf.hashtags h
//    WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
//    ORDER BY h.name ASC
//    """)
//    Page<String> suggestHashtag(@Param("keyword") String keyword, Pageable pageable);
}
