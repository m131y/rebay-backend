package com.rebay.rebay_backend.integratedProduct.service;

import com.rebay.rebay_backend.integratedProduct.dto.ProductFeedItem;
import com.rebay.rebay_backend.integratedProduct.dto.ProductType;
import com.rebay.rebay_backend.integratedProduct.repository.IntegratedProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IntegratedProductService {
    private final IntegratedProductRepository integratedProductRepository;

    public Page<ProductFeedItem> getProductFeed(
            int page,
            int size,
            Integer categoryCode,
            String sort,
            Boolean excludeSold,
            ProductType productType
    ) {
        System.out.println(page);
        System.out.println(size);
        System.out.println(categoryCode);
        System.out.println(excludeSold);
        System.out.println(sort);
        System.out.println(productType);
        return integratedProductRepository.findIntegratedProducts(
                page, size, categoryCode, sort, excludeSold, productType
        );
    }
}