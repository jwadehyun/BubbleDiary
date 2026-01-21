package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageRepository messageRepo;

    public MessageController(MessageRepository messageRepo) {
        this.messageRepo = messageRepo;
    }

    // POST /api/messages
    @PostMapping
    public Message createMessage(@RequestBody CreateMessageRequest request) {
        Message message = new Message(request.getText(), java.time.Instant.now(), request.getUser());
        return messageRepo.save(message);
    }

    // GET /api/messages
    @GetMapping
    public List<Message> getMessages() {
        return messageRepo.findAll();
    }

    // GET /api/messages/{id}
    @GetMapping("/{id}")
    public Message getMessageById(@PathVariable Long id) throws MessageNotFoundException {
        return messageRepo.findById(id)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with id " + id));
    }

}
