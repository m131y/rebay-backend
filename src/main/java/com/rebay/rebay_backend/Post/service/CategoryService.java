package com.rebay.rebay_backend.Post.service;

import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Integer> getDescendantCodes(Integer categoryCode) {
        Category startCategory = categoryRepository.findWithChildrenByCode(categoryCode)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with code: " + categoryCode));

        List<Integer> descendantCodes = new ArrayList<>();
        collectDescendantCodes(startCategory, descendantCodes);

        return descendantCodes;
    }

    private void collectDescendantCodes(Category category, List<Integer> codes) {
        codes.add(category.getCode());
        Set<Category> children = category.getChildren();

        if (children != null) {
            for (Category child : children) {
                collectDescendantCodes(child, codes);
            }
        }
    }
}
