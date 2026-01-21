package com.example;

public class CreateMessageRequest {
    private User user; // should be changed to userId in the future
    private String text;

    public CreateMessageRequest() {
    }

    public CreateMessageRequest(User user, String text) {
        this.user = user;
        this.text = text;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
