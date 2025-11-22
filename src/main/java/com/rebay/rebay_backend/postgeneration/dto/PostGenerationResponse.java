package com.rebay.rebay_backend.postgeneration.dto;

import lombok.Data;

@Data
public class PostGenerationResponse {
    private String title;        // 추천 제목
    private String content;      // 추천 본문
    private Integer categoryCode; // 추천 카테고리 코드
}