package com.example;

import java.time.Instant;

public class Message {

    private String text;
    private Long messageId;
    private Instant createdAt;

    public Message() {
    }

    public Message(String text, Long messageId, Instant createdAt) {
        this.text = text;
        this.messageId = messageId;
        this.createdAt = createdAt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
