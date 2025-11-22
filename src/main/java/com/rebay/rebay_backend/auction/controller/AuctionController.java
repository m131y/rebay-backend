package com.rebay.rebay_backend.auction.controller;

import com.rebay.rebay_backend.auction.dto.AuctionRequest;
import com.rebay.rebay_backend.auction.dto.AuctionResponse;
import com.rebay.rebay_backend.auction.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auction")
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(@Valid @RequestBody AuctionRequest request) {
        return ResponseEntity.ok(auctionService.createAuction(request));
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.getAuction(auctionId));
    }

    @GetMapping
    public ResponseEntity<Page<AuctionResponse>> getAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAuctions(pageable);

        return ResponseEntity.ok(auctions);
    }

    @PutMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> updateAuction(
            @PathVariable Long auctionId,
            @Valid @RequestBody AuctionRequest request
    ) {
        return ResponseEntity.ok(auctionService.updateAuction(auctionId, request));
    }

    @DeleteMapping("/{auctionId}")
    public ResponseEntity<Void> deleteAuction(@PathVariable Long auctionId) {
        auctionService.deleteAuction(auctionId);
        return ResponseEntity.noContent().build();
    }
}
