package com.rebay.rebay_backend.S3.controller;

import com.rebay.rebay_backend.S3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class S3Controller {

    private final S3Service s3Service;

    private static final int EXPIRATION_MINUTES=60;

    @PostMapping("/post")
    public ResponseEntity<Map<String, String>> uploadPostImage(@RequestParam("file") MultipartFile file) {
        String url = s3Service.uploadFile(file, "ssh/post"); //
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/profile")
    public ResponseEntity<Map<String, String>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        String url = s3Service.uploadFile(file, "ssh/profile"); //
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/post/image")
    public ResponseEntity<Map<String,String>> getPresignedUrl(@RequestParam String url) {
        String imageUrl = s3Service.generatePresignedUrl(url, EXPIRATION_MINUTES);
        return ResponseEntity.ok(Map.of("imageUrl" ,imageUrl));
    }
}
