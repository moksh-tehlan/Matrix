package com.paperlink.server.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.ai.chat.messages.MessageType;

/**
 * Entity for chat messages
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "chats")
@Builder
@AllArgsConstructor
@ToString(callSuper=true, exclude = "user")
@EqualsAndHashCode(callSuper=true, exclude = "user")
public class ChatEntity extends BaseEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private String userId;

    @Column(name = "conversation_id")
    private String conversationId;

    /**
     * Create a chat entity for a specific role
     *
     * @param message Message content
     * @param role Message role
     * @param user User entity
     * @param conversationId Conversation ID
     * @return New chat entity
     */
    public static ChatEntity of(String message, MessageType role, UserEntity user, String conversationId) {
        return ChatEntity.builder()
                .message(message)
                .role(role)
                .user(user)
                .conversationId(conversationId)
                .build();
    }
}