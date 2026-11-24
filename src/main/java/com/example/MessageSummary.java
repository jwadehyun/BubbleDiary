package com.example;

import java.time.Instant;

/** A message as the front end sees it: no photo bytes or replies, just a link and a count. */
public record MessageSummary(
        Long id,
        String text,
        String imageUrl,
        long time,
        String status,
        String error,
        long replyCount) {

    // Used by the JPQL query in MessageRepository.
    public MessageSummary(Long id, String text, String imageType, Instant createdAt, MessageStatus status,
                          String error, Long replyCount) {
        this(id, text == null ? "" : text, imageType == null ? null : "/api/messages/" + id + "/image",
                createdAt.toEpochMilli(), status.name().toLowerCase(), error, replyCount);
    }
}
