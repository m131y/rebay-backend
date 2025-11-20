package com.rebay.rebay_backend.chat.service;

import com.rebay.rebay_backend.chat.dto.ChatSendRequest;
import com.rebay.rebay_backend.chat.dto.RoomMessageEvent;
import com.rebay.rebay_backend.chat.entity.ChatMessage;
import com.rebay.rebay_backend.chat.entity.ChatMessage.MessageType;
import com.rebay.rebay_backend.chat.redis.RedisPublisher;
import com.rebay.rebay_backend.chat.repository.ChatMessageRepository;
import com.rebay.rebay_backend.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rebay.rebay_backend.chat.entity.ChatRoom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisPublisher redisPublisher;

    @Transactional
    public void send(Long roomId, Long senderId, ChatSendRequest req) {
        if (!chatRoomRepository.existsByIdAndParticipant(roomId, senderId)) {
            throw new AccessDeniedException("Not a participant of room " + roomId);
        }

        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.builder()
                        .roomId(roomId)
                        .senderId(senderId)
                        .messageType(parseType(req.type()))
                        .content(req.content())
                        .build()
        );

        RoomMessageEvent ev = new RoomMessageEvent(
                saved.getRoomId(),
                saved.getId(),
                saved.getSenderId(),
                saved.getMessageType().name(),
                saved.getContent(),
                saved.getCreatedAt()
        );
        redisPublisher.publishRoom(roomId, ev);
    }

    @Transactional
    public Long getOrCreateChatRoom(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신과는 채팅할 수 없습니다.");
        }

        // 이미 존재하는 방이 있는지 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository.findAll().stream()
                .filter(r -> (r.getParticipant1Id().equals(userId) && r.getParticipant2Id().equals(targetUserId)) ||
                        (r.getParticipant1Id().equals(targetUserId) && r.getParticipant2Id().equals(userId)))
                .findFirst();

        if (existingRoom.isPresent()) {
            return existingRoom.get().getId();
        }

        // 없으면 새로 생성
        ChatRoom newRoom = ChatRoom.create(userId, targetUserId);
        chatRoomRepository.save(newRoom);
        return newRoom.getId();
    }

    private static MessageType parseType(String type) {
        if (type == null) return MessageType.TEXT;
        try { return MessageType.valueOf(type.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return MessageType.TEXT; }
    }
}
