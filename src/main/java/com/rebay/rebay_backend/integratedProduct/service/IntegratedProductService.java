package com.rebay.rebay_backend.integratedProduct.service;

import com.rebay.rebay_backend.Post.dto.PostResponse;
import com.rebay.rebay_backend.Post.entity.Post;
import com.rebay.rebay_backend.Post.entity.SaleStatus;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import com.rebay.rebay_backend.Post.service.CategoryService;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.entity.Auction;
import com.rebay.rebay_backend.integratedProduct.dto.IntegratedProductResponse;
import com.rebay.rebay_backend.integratedProduct.dto.ProductFeedItem;
import com.rebay.rebay_backend.integratedProduct.dto.ProductType;
import com.rebay.rebay_backend.integratedProduct.entity.IntegratedProductView;
import com.rebay.rebay_backend.integratedProduct.repository.IntegratedProductRepository;
import com.rebay.rebay_backend.integratedProduct.repository.IntegratedProductViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IntegratedProductService {
    private final IntegratedProductRepository integratedProductRepository;
    private final IntegratedProductViewRepository integratedProductViewRepository;
    private final CategoryService categoryService;

    // post + auction 통합 필터링, 정렬 조회 (상품보기)
    public Page<ProductFeedItem> getProductFeed(
            int page,
            int size,
            Integer categoryCode,
            String sort,
            Boolean excludeSold,
            ProductType productType
    ) {
        Collection<SaleStatus> requiredStatuses;

        if (Objects.equals(excludeSold, Boolean.TRUE)) {
            requiredStatuses = List.of(SaleStatus.ON_SALE);
        } else {
            requiredStatuses = List.of(SaleStatus.ON_SALE, SaleStatus.SOLD);
        }

        Pageable pageable = createPageable(page, size, sort);
        Page<IntegratedProductView> viewPage;

        if (categoryCode != null) {
            List<Integer> categoryCodes = categoryService.getDescendantCodes(categoryCode);

            if (productType != null) {
                viewPage = integratedProductViewRepository.findByIdProductTypeAndCategoryCodeInAndStatusIn(
                        productType,
                        categoryCodes,
                        requiredStatuses,
                        pageable
                );
            } else {
                viewPage = integratedProductViewRepository.findByCategoryCodeInAndStatusIn(
                        categoryCodes,
                        requiredStatuses,
                        pageable
                );
            }
        } else if (productType != null) {
            viewPage = integratedProductViewRepository.findByIdProductTypeAndStatusIn(
                    productType,
                    requiredStatuses,
                    pageable
            );
        } else {
            viewPage = integratedProductViewRepository.findByStatusIn(requiredStatuses, pageable);
        }

        return viewPage.map(this::mapViewToProductFeedItem);
    }

    // 사용자 별 post + auction 통합 조회 (마이페이지 - 내 상품)
    public List<ProductFeedItem> findUserIntegratedProducts(Long userId) {
        List<IntegratedProductView> viewList = integratedProductViewRepository.findBySellerIdOrderByCreatedAtDesc(userId);
        return viewList.stream().map(this::mapViewToProductFeedItem).toList();
    }

    public ProductFeedItem mapViewToProductFeedItem(IntegratedProductView view) {
        ProductType type = view.getId().getProductType();

        return ProductFeedItem.builder()
                .productId(view.getId().getProductId())
                .productType(type)
                .title(view.getTitle())
                .content(view.getContent())
                .categoryCode(view.getCategoryCode())
                .status(view.getStatus())
                .createdAt(view.getCreatedAt())
                .thumbnailImageUrl(view.getImageUrl())
                .price(type == ProductType.POST ? view.getPrice() : null)
                .currentPrice(type == ProductType.AUCTION ? view.getPrice() : null) // AUCTION의 price는 currentPrice로 사용
                .build();
    }

    private Pageable createPageable(int page, int size, String sort) {
        Sort.Direction direction = Sort.Direction.DESC;
        String property = "createdAt";

        if (sort != null) {
            String sortUpper = sort.toUpperCase();
            if (sortUpper.startsWith("PRICE")) {
                property = "price"; // 뷰의 price 필드 사용
                direction = sortUpper.endsWith("_ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            } else if (sortUpper.equals("LATEST")) {
                property = "createdAt";
                direction = Sort.Direction.DESC;
            }
        }

        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}