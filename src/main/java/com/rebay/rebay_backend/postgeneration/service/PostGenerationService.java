package com.rebay.rebay_backend.postgeneration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rebay.rebay_backend.Post.entity.Category;
import com.rebay.rebay_backend.Post.repository.CategoryRepository;
import com.rebay.rebay_backend.postgeneration.dto.PostGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostGenerationService {

    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    public PostGenerationResponse analyzeImage(MultipartFile imageFile) throws IOException {
        // 이미지 인코딩
        String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
        String imageUrl = "data:image/jpeg;base64," + base64Image;

        // 카테고리 정보 준비
        List<Category> categories = categoryRepository.findAll();
        String categoryListPrompt = categories.stream()
                .map(c -> c.getCode() + ":" + c.getName())
                .collect(Collectors.joining(", "));

        // 시스템 프롬프트
        String systemPrompt = """
            당신은 센스 있고 친절한 중고거래 판매 전문가입니다.
            제공된 상품 사진을 보고 사고 싶어지는 매력적인 판매글을 작성해주세요.
            
            [작성 가이드라인]
            1. **제목**: 브랜드명, 모델명, 핵심 특징을 포함하여 20~40자 이내로 간결하게 작성하세요.
            2. **내용**: 다음 내용을 포함하여 3~5문단으로 상세하게 작성하세요 (존댓말 사용).
               - **첫인사**: 가볍고 친절한 인사.
               - **상품 상세 설명**: 외관 상태(스크래치 등), 색상, 주요 스펙, 사용감 정도.
               - **추천**: 어떤 분에게 추천하는지.
               - **마무리**: 친절한 맺음말.
            3. **카테고리**: 아래 [카테고리 목록] 중 사진과 가장 일치하는 항목의 '숫자 코드'를 정확히 선택하세요.
            
            [카테고리 목록]
            %s
            """.formatted(categoryListPrompt);

        // Structured Output을 위한 JSON Schema 정의
        Map<String, Object> jsonSchema = Map.of(
                "name", "product_analysis_response",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "title", Map.of(
                                        "type", "string",
                                        "description", "상품명, 브랜드, 상태급(S급/A급)을 포함한 매력적인 제목"
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "인사말, 상세 상태, 특징, 추천 대상을 포함한 200자 이상의 친절하고 상세한 판매글 본문"
                                ),
                                "categoryCode", Map.of(
                                        "type", "integer",
                                        "description", "제공된 카테고리 목록 중 가장 적합한 코드 (반드시 목록에 있는 숫자여야 함)"
                                )
                        ),
                        "required", List.of("title", "content", "categoryCode"), // 필수 필드 지정
                        "additionalProperties", false // 정의되지 않은 필드 금지
                )
        );

        // 5. 요청 본문 구성
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", "이 물건을 팔려고 합니다. 정보를 분석해주세요."),
                                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
                        ))
                ),
                // Structured Output
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", jsonSchema
                ),
                "max_tokens", 1000
        );

        // 6. API 호출
        WebClient webClient = WebClient.builder()
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        String responseJson = webClient.post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseResponse(responseJson);
    }

    private PostGenerationResponse parseResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String contentString = root.path("choices").get(0).path("message").path("content").asText();
            return objectMapper.readValue(contentString, PostGenerationResponse.class);
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패", e);
            throw new RuntimeException("AI 분석 결과를 처리하는 중 오류가 발생했습니다.");
        }
    }
}