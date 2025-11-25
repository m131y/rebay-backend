package com.rebay.rebay_backend.Post.repository;

import com.rebay.rebay_backend.Post.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCode(int categoryCode);

    @EntityGraph(attributePaths = {"children"})
    Optional<Category> findWithChildrenByCode(int code);
}
