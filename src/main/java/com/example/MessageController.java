package com.example;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/** The signed-in user's diary entries. Every endpoint only sees the caller's own messages. */
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messages;
    private final ReplyGenerator generator;

    public MessageController(MessageService messages, ReplyGenerator generator) {
        this.messages = messages;
        this.generator = generator;
    }

    // POST /api/messages: saves the entry with generic replies, then AI replies arrive in the background
    @PostMapping
    public MessageSummary createMessage(@CurrentUser User user, @RequestBody CreateMessageRequest request) {
        Long id = messages.create(user, request);
        MessageSummary summary = messages.get(id, user);
        if (summary.status().equals("pending")) generator.generate(id);
        return summary;
    }

    // GET /api/messages
    @GetMapping
    public List<MessageSummary> getMessages(@CurrentUser User user) {
        return messages.list(user);
    }

    // GET /api/messages/{id}: poll this while status is "pending"
    @GetMapping("/{id}")
    public MessageSummary getMessageById(@CurrentUser User user, @PathVariable Long id) {
        return messages.get(id, user);
    }

    // GET /api/messages/{id}/replies
    @GetMapping("/{id}/replies")
    public List<ReplyView> getReplies(@CurrentUser User user, @PathVariable Long id) {
        return messages.replies(id, user);
    }

    // GET /api/messages/{id}/image
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@CurrentUser User user, @PathVariable Long id) {
        Message message = messages.getWithImage(id, user);
        if (message.getImage() == null) throw new MessageNotFoundException(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(message.getImageType()))
                // A message's photo never changes.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(message.getImage());
    }

    // POST /api/messages/{id}/retry: rewrites the AI replies
    @PostMapping("/{id}/retry")
    public MessageSummary retry(@CurrentUser User user, @PathVariable Long id) {
        if (messages.get(id, user).status().equals("pending")) return messages.get(id, user);
        messages.resetForRetry(id, user);
        generator.generate(id);
        return messages.get(id, user);
    }

    // DELETE /api/messages/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@CurrentUser User user, @PathVariable Long id) {
        messages.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/messages
    @DeleteMapping
    public ResponseEntity<Void> deleteAll(@CurrentUser User user) {
        messages.deleteAll(user);
        return ResponseEntity.noContent().build();
    }
}
