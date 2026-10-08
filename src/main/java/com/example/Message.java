package com.example;

import jakarta.persistence.*;
import java.time.Instant;

/** A diary entry the user posted, as if they were the celebrity. Fans reply to it. */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Empty for a photo-only post.
    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "LONGBLOB")
    private byte[] image;

    // MIME type of the image, e.g. "image/jpeg"; null when there's no photo.
    private String imageType;

    @Column(nullable=false)
    private Instant createdAt;

    // Progress of the AI fan replies.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.PENDING;

    @Column(length = 1000)
    private String error;

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

    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }

    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public User getUser() { return user;}
    public void setUser(User user) { this.user = user; }
}
