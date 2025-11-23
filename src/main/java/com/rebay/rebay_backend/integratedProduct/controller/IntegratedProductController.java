package com.rebay.rebay_backend.integratedProduct.controller;

import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.integratedProduct.dto.ProductFeedItem;
import com.rebay.rebay_backend.integratedProduct.dto.ProductType;
import com.rebay.rebay_backend.integratedProduct.service.IntegratedProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Slf4j
public class IntegratedProductController {
    private final IntegratedProductService integratedProductService;

    @GetMapping
    public ResponseEntity<Page<ProductFeedItem>> getProductFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer categoryCode,
            @RequestParam(defaultValue = "LATEST") String sort, // LATEST, PRICE_ASC, PRICE_DESC, TITLE_ASC
            @RequestParam(defaultValue = "true") Boolean excludeSold, // true면 판매 완료 제외
            @RequestParam(required = false) ProductType productType
    ) {
        Page<ProductFeedItem> productFeedItems = integratedProductService.getProductFeed(
                page, size, categoryCode, sort, excludeSold, productType
        );
        return ResponseEntity.ok(productFeedItems);
    }
}