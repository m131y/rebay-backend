package com.rebay.rebay_backend.chat.repository;

import com.rebay.rebay_backend.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);
    // Page<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId, Pageable pageable);

    // 특정 방의 가장 최신 메시지 1개 조회(채팅방의 마지막 메시지를 가져오기 위한 메서드)
    Optional<ChatMessage> findFirstByRoomIdOrderByIdDesc(Long roomId);
}
