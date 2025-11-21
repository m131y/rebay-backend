package com.rebay.rebay_backend.chat.service;

import com.rebay.rebay_backend.chat.dto.ChatRoomDto;
import com.rebay.rebay_backend.chat.dto.ChatSendRequest;
import com.rebay.rebay_backend.chat.dto.RoomMessageEvent;
import com.rebay.rebay_backend.chat.entity.ChatMessage;
import com.rebay.rebay_backend.chat.entity.ChatMessage.MessageType;
import com.rebay.rebay_backend.chat.redis.RedisPublisher;
import com.rebay.rebay_backend.chat.repository.ChatMessageRepository;
import com.rebay.rebay_backend.chat.repository.ChatRoomRepository;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rebay.rebay_backend.chat.entity.ChatRoom;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisPublisher redisPublisher;
    private final UserRepository userRepository;

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

    // 내 채팅방 목록 조회
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getChatRooms(Long userId) {
        // 내가 참여한 방 찾기 (참여자1 or 참여자2)
        List<ChatRoom> rooms = chatRoomRepository.findByParticipant1IdOrParticipant2Id(userId, userId);

        return rooms.stream().map(room -> {
                    // 상대방 Id 찾기
                    Long partnerId = room.getParticipant1Id().equals(userId) ?
                            room.getParticipant2Id() : room.getParticipant1Id();

                    // 상대방 정보 조회
                    User partner = userRepository.findById(partnerId).orElse(null);
                    String partnerName = (partner != null) ? partner.getUsername() : "알 수 없음";
                    String partnerImage = (partner != null) ? partner.getProfileImageUrl() : null;

                    // 마지막 메시지 조회
                    ChatMessage lastMsg = chatMessageRepository.findFirstByRoomIdOrderByIdDesc(room.getId())
                            .orElse(null);

                    return ChatRoomDto.builder()
                            .roomId(room.getId())
                            .partnerId(partnerId)
                            .partnerName(partnerName)
                            .partnerImage(partnerImage)
                            .lastMessage(lastMsg != null ? lastMsg.getContent() : "")
                            .lastMessageTime(lastMsg != null ? lastMsg.getCreatedAt() : room.getCreatedAt())
                            .build();
                })
                // 최신 메시지가 있는 방을 위로 정렬
                .sorted((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()))
                .collect(Collectors.toList());
    }

    private static MessageType parseType(String type) {
        if (type == null) return MessageType.TEXT;
        try { return MessageType.valueOf(type.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return MessageType.TEXT; }
    }
}
