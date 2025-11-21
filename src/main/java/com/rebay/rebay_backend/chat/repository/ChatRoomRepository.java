package com.rebay.rebay_backend.chat.repository;


import com.rebay.rebay_backend.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 내가 참여한 모든 방 찾기 (참여자1 또는 참여자2가 나인 경우)
    List<ChatRoom> findByParticipant1IdOrParticipant2Id(Long participant1Id, Long participant2Id);

    @Query("""
           select case when count(r)>0 then true else false end
           from ChatRoom r
           where r.id = :roomId
             and (r.participant1Id = :userId or r.participant2Id = :userId)
           """)
    boolean existsByIdAndParticipant(Long roomId, Long userId);
}