package com.example;

public class CreateMessageRequest {
    private String text;
    // Optional photo as a data URL, e.g. "data:image/jpeg;base64,..."
    private String image;

    public CreateMessageRequest() {
    }

    public CreateMessageRequest(String text, String image) {
        this.text = text;
        this.image = image;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
