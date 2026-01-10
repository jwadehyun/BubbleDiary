package com.example;

public class CreateMessageRequest {
    private String text;

    public CreateMessageRequest() {
    }

    public CreateMessageRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
