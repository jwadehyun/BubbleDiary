package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepo;

    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // POST /api/users
    @PostMapping
    public User createUser(@RequestBody CreateUserRequest request) {
        User user = new User(request.getUsername(), request.getPassword());
        return userRepo.save(user);
    }

    // GET /api/users
    @GetMapping
    public List<User> getUsers() {
        return userRepo.findAll();
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public User getMessageById(@PathVariable Long id) throws MessageNotFoundException {
        return userRepo.findById(id)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with id " + id));
    }

}
