package com.paperlink.server.services;

import com.paperlink.server.entities.ChatEntity;
import com.paperlink.server.repositories.ChatRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
  private final ChatRepository chatRepository;

  public void saveChatMessages(List<ChatEntity> chats) {
    chatRepository.saveAll(chats);
  }

  public List<ChatEntity> getChatsByUserId(String userId) {
    return chatRepository.findByUserIdOrderByCreatedAtAsc(userId);
  }

  public void deleteChatByUserId(String userId) {
    chatRepository.deleteByUserId(userId);
  }
}
