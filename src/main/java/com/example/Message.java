package com.example;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable=false)
    private String text;

    @Column(nullable=false)
    private Instant createdAt;

    public Message() {
    }

    public Message(String text, Instant createdAt, User user) {
        this.text = text;
        this.createdAt = createdAt;
        this.user = user;
    }

    public Long getMessageId() { return messageId;}
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public String getText() { return text; }
    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() { return user;}
    public void setUser(User user) { this.user = user; }
}
