package com.example;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    // In-memory storage for now
    private final List<Message> messages = new CopyOnWriteArrayList<>(); //thread-safe list
    private final AtomicLong idGenerator = new AtomicLong(0); //thread-safe increment counter

    // POST /api/messages
    @PostMapping
    public Message createMessage(@RequestBody CreateMessageRequest request) {
        Long id = idGenerator.incrementAndGet();
        Message message = new Message(request.getText(), id, java.time.Instant.now());
        messages.add(message);
        return message;
    }

    // GET /api/messages
    @GetMapping
    public List<Message> getMessages() {
        return messages;
    }

    // GET /api/messages/{id}
    @GetMapping("/{id}")
    public Message getMessageById(@PathVariable Long id) throws MessageNotFoundException {
        return messages.stream()
                .filter(msg -> msg.getMessageId().equals(id))
                .findFirst()
                .orElseThrow(() -> new MessageNotFoundException("Message not found with id: " + id));
    }

}
