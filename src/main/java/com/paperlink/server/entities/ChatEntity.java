package com.paperlink.server.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.ai.chat.messages.MessageType;

@Data
@NoArgsConstructor
@Entity
@Table(name = "chats")
@Builder
@AllArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
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

    @Column(name = "conversation_id")
    private String conversationId;
}