package com.rebay.rebay_backend.chat.controller;

import com.rebay.rebay_backend.chat.dto.ChatRoomDto;
import com.rebay.rebay_backend.chat.dto.RoomMessageEvent;
import com.rebay.rebay_backend.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import com.rebay.rebay_backend.chat.service.ChatService;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.service.AuthenticationService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatMessageRepository repo;

    private final ChatService chatService;
    private final AuthenticationService authenticationService;

    @GetMapping("/rooms/{roomId}/messages")
    public Page<RoomMessageEvent> getMessagesByRoomId(@PathVariable Long roomId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "30") int size) {
        return repo.findByRoomIdOrderByIdDesc(roomId, PageRequest.of(page, size))
                .map(m -> new RoomMessageEvent(
                        m.getRoomId(),
                        m.getId(),
                        m.getSenderId(),
                        m.getMessageType().name(),
                        m.getContent(),
                        m.getCreatedAt()
                ));
    }

    // 채팅방 생성/입장
    @PostMapping("/rooms")
    public Long createOrGetRoom(@RequestParam Long targetUserId) {
        User currentUser = authenticationService.getCurrentUser();
        return chatService.getOrCreateChatRoom(currentUser.getId(), targetUserId);
    }

    // 내 채팅방 목록 조회
    @GetMapping("/rooms")
    public List<ChatRoomDto> getMyChatRooms() {
        User currentUser = authenticationService.getCurrentUser();
        return chatService.getChatRooms(currentUser.getId());
    }

}