package com.knowledgehub.domain.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    Page<ChatSession> findByUserId(String userId, Pageable pageable);

    Optional<ChatSession> findByIdAndUserId(String id, String userId);

    List<ChatSession> findTop5ByUserIdOrderByUpdatedAtDesc(String userId);

    long countByUserId(String userId);
}
