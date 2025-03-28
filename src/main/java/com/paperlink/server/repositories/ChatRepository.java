package com.paperlink.server.repositories;

import com.paperlink.server.entities.ChatEntity;
import com.paperlink.server.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<ChatEntity, String> {
    List<ChatEntity> findByUserIdOrderByCreatedAtAsc(String userId);

    void deleteByUserId(String userId);

    String user(UserEntity user);
}
