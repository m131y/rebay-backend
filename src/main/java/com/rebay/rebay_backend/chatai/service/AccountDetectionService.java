package com.rebay.rebay_backend.chatai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rebay.rebay_backend.chatai.dto.AccountCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDetectionService {

    private final WebClient.Builder webClientBuilder;

    @Value("${openai.keys.chat}")
    private String apiKey;

    public AccountCheckResponse detect(String text) {

        WebClient client = webClientBuilder
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .baseUrl("https://api.openai.com/v1")
                .build();

        var requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system",
                                "content", "사용자 입력이 은행명, 은행 계좌번호 또는 금융정보 공유인지 판단하여 YES 또는 NO만 출력하세요. 계좌, 이체 같은 키워드도 금융정보 공유에 포함됩니다."),
                        Map.of("role", "user", "content", text)
                )
        );

        String aiResponse = client.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // 파싱
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(aiResponse);
        } catch (Exception e) {
            return new AccountCheckResponse(false, "AI 파싱 실패");
        }

        String content = root.path("choices").get(0).path("message").path("content").asText();

        boolean isAccount = content.trim().equalsIgnoreCase("YES");

        return new AccountCheckResponse(isAccount, content);
    }
}

