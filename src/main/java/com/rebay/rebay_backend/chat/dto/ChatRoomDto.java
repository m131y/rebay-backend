package com.rebay.rebay_backend.chat.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomDto {
    private Long roomId;
    private Long partnerId;
    private String partnerName;
    private String partnerImage;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
}