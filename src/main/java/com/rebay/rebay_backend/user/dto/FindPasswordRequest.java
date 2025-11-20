package com.rebay.rebay_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FindPasswordRequest {
    private String email;
    private String username;
}
