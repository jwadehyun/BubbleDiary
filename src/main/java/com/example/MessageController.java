package com.example;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/** The signed-in user's diary entries. Every endpoint only sees the caller's own messages. */
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messages;
    private final UserService users;
    private final ReplyGenerator generator;

    public MessageController(MessageService messages, UserService users, ReplyGenerator generator) {
        this.messages = messages;
        this.users = users;
        this.generator = generator;
    }

    // POST /api/messages: saves the entry with generic replies, then AI replies arrive in the background
    @PostMapping
    public MessageSummary createMessage(OAuth2AuthenticationToken auth, @RequestBody CreateMessageRequest request) {
        User user = users.require(auth);
        Long id = messages.create(user, request);
        MessageSummary summary = messages.get(id, user);
        if (summary.status().equals("pending")) generator.generate(id);
        return summary;
    }

    // GET /api/messages
    @GetMapping
    public List<MessageSummary> getMessages(OAuth2AuthenticationToken auth) {
        return messages.list(users.require(auth));
    }

    // GET /api/messages/{id}: poll this while status is "pending"
    @GetMapping("/{id}")
    public MessageSummary getMessageById(OAuth2AuthenticationToken auth, @PathVariable Long id) {
        return messages.get(id, users.require(auth));
    }

    // GET /api/messages/{id}/replies
    @GetMapping("/{id}/replies")
    public List<ReplyView> getReplies(OAuth2AuthenticationToken auth, @PathVariable Long id) {
        return messages.replies(id, users.require(auth));
    }

    // GET /api/messages/{id}/image
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(OAuth2AuthenticationToken auth, @PathVariable Long id) {
        Message message = messages.getWithImage(id, users.require(auth));
        if (message.getImage() == null) throw new MessageNotFoundException(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(message.getImageType()))
                // A message's photo never changes.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(message.getImage());
    }

    // POST /api/messages/{id}/retry: rewrites the AI replies
    @PostMapping("/{id}/retry")
    public MessageSummary retry(OAuth2AuthenticationToken auth, @PathVariable Long id) {
        User user = users.require(auth);
        if (messages.get(id, user).status().equals("pending")) return messages.get(id, user);
        messages.resetForRetry(id, user);
        generator.generate(id);
        return messages.get(id, user);
    }

    // DELETE /api/messages/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(OAuth2AuthenticationToken auth, @PathVariable Long id) {
        messages.delete(id, users.require(auth));
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/messages
    @DeleteMapping
    public ResponseEntity<Void> deleteAll(OAuth2AuthenticationToken auth) {
        messages.deleteAll(users.require(auth));
        return ResponseEntity.noContent().build();
    }
}
