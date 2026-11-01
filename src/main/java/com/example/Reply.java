package com.example;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/** A fan's reply to a message: either written by the AI or a quick generic reaction. */
@Entity
@Table(name = "replies", indexes = @Index(columnList = "message_id"))
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long replyId;

    // The database deletes replies along with their message.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Message message;

    @Column(nullable = false)
    private String fan;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    private int likes;

    // true for AI-written replies, false for quick generic reactions
    private boolean ai;

    public Reply() {
    }

    public Reply(Message message, String fan, String text, Instant createdAt, int likes, boolean ai) {
        this.message = message;
        this.fan = fan;
        this.text = text;
        this.createdAt = createdAt;
        this.likes = likes;
        this.ai = ai;
    }

    public Long getReplyId() { return replyId; }
    public Message getMessage() { return message; }
    public String getFan() { return fan; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
    public int getLikes() { return likes; }
    public boolean isAi() { return ai; }
}
