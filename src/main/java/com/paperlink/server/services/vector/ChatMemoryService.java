package com.paperlink.server.services.vector;

import com.paperlink.server.entities.ChatEntity;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.ResourceNotFoundException;
import com.paperlink.server.services.ChatService;
import com.paperlink.server.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Spring AI's ChatMemory interface for persisting chat history
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMemoryService implements ChatMemory {
    private final ChatService chatService;
    private final UserService userService;

    /**
     * Add messages to the chat memory
     *
     * @param conversationId Conversation ID (user ID)
     * @param messages List of messages to add
     */
    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        long startTime = System.currentTimeMillis();
        log.debug("Adding {} messages to chat memory for conversation: {}", messages.size(), conversationId);

        try {
            UserEntity user = userService.findById(conversationId);

            List<ChatEntity> chatMessages = messages.stream()
                    .map(message -> ChatEntity.builder()
                            .role(message.getMessageType())
                            .message(message.getText())
                            .user(user)
                            .conversationId(conversationId)
                            .build())
                    .toList();

            chatService.saveChatMessages(chatMessages);

            long endTime = System.currentTimeMillis();
            log.debug("Added messages to chat memory in {} ms", (endTime - startTime));
        } catch (ResourceNotFoundException e) {
            log.error("Failed to add messages: User not found with ID: {}", conversationId, e);
            throw e;
        } catch (Exception e) {
            log.error("Error adding messages to chat memory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add messages to chat memory", e);
        }
    }

    /**
     * Get messages from the chat memory
     *
     * @param conversationId Conversation ID (user ID)
     * @param lastN Number of messages to retrieve (0 for all)
     * @return List of messages
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "chatMessages", key = "#conversationId", unless = "#result.isEmpty()")
    public List<Message> get(String conversationId, int lastN) {
        long startTime = System.currentTimeMillis();
        log.debug("Retrieving messages from chat memory for conversation: {}, lastN: {}", conversationId, lastN);

        try {
            List<ChatEntity> chatMessages = chatService.getChatsByUserId(conversationId);
            List<Message> messages = new ArrayList<>();

            for (ChatEntity chatMessage : chatMessages) {
                switch (chatMessage.getRole()) {
                    case USER -> messages.add(new UserMessage(chatMessage.getMessage()));
                    case ASSISTANT -> messages.add(new AssistantMessage(chatMessage.getMessage()));
                    case SYSTEM -> messages.add(new SystemMessage(chatMessage.getMessage()));
                    default -> log.warn("Unknown message type: {}", chatMessage.getRole());
                }
            }

            // If lastN > 0, return only the last N messages
            if (lastN > 0 && messages.size() > lastN) {
                messages = messages.subList(messages.size() - lastN, messages.size());
            }

            long endTime = System.currentTimeMillis();
            log.debug("Retrieved {} messages from chat memory in {} ms", messages.size(), (endTime - startTime));

            return messages;
        } catch (Exception e) {
            log.error("Error retrieving messages from chat memory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve messages from chat memory", e);
        }
    }

    /**
     * Clear all messages for a conversation
     *
     * @param conversationId Conversation ID (user ID)
     */
    @Override
    @Transactional
    @CacheEvict(value = "chatMessages", key = "#conversationId")
    public void clear(String conversationId) {
        log.debug("Clearing chat memory for conversation: {}", conversationId);

        try {
            chatService.deleteChatByUserId(conversationId);
            log.info("Chat memory cleared for conversation: {}", conversationId);
        } catch (Exception e) {
            log.error("Error clearing chat memory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear chat memory", e);
        }
    }
}
