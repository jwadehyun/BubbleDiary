package com.example;

import jakarta.persistence.*;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // OAuth identity: the login provider ("google", "github") and that provider's id for the user.
    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    private String email;

    private String name;

    // Diary settings
    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String fandom = "Bubblies";

    // An image URL from the OAuth provider, or a data URL the user uploaded.
    @Column(columnDefinition = "LONGTEXT")
    private String avatar;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FanVoice voice = FanVoice.ENGLISH;

    private int replyCount = 40;

    private int fillerCount = 250;

    public User() {
    }

    public User(String provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    // Getters and Setters
    public Long getId() { return userId; }
    public void setId(Long userId) { this.userId = userId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getFandom() { return fandom; }
    public void setFandom(String fandom) { this.fandom = fandom; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public FanVoice getVoice() { return voice; }
    public void setVoice(FanVoice voice) { this.voice = voice; }

    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }

    public int getFillerCount() { return fillerCount; }
    public void setFillerCount(int fillerCount) { this.fillerCount = fillerCount; }
}
