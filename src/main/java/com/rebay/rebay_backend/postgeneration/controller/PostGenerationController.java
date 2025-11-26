package com.rebay.rebay_backend.postgeneration.controller;

import com.rebay.rebay_backend.postgeneration.dto.PostGenerationResponse;
import com.rebay.rebay_backend.postgeneration.service.PostGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class PostGenerationController {

    private final PostGenerationService postGenerationService;

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<PostGenerationResponse> analyzeImage(@RequestParam("file") MultipartFile file) {
        try {
            PostGenerationResponse response = postGenerationService.analyzeImage(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}