package com.rebay.rebay_backend.chatai.controller;

import com.rebay.rebay_backend.chatai.dto.AccountCheckRequest;
import com.rebay.rebay_backend.chatai.dto.AccountCheckResponse;
import com.rebay.rebay_backend.chatai.service.AccountDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatai/detect")
@RequiredArgsConstructor
public class AccountDetectionController {

    private final AccountDetectionService service;

    @PostMapping("/account")
    public AccountCheckResponse check(@RequestBody AccountCheckRequest request) {
        return service.detect(request.message());
    }
}