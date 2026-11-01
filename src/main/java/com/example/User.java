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

    // Shown under the name, like a messenger status ("in Tokyo 🗼").
    @Column(length = 60)
    private String statusText;

    @Column(length = 150)
    private String profileMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FanVoice voice = FanVoice.ENGLISH;

    // How the user wants their fans to talk, added to the AI prompt.
    @Column(length = 300)
    private String fanPrompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.FREE;

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

    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }

    public String getProfileMessage() { return profileMessage; }
    public void setProfileMessage(String profileMessage) { this.profileMessage = profileMessage; }

    public FanVoice getVoice() { return voice; }
    public void setVoice(FanVoice voice) { this.voice = voice; }

    public String getFanPrompt() { return fanPrompt; }
    public void setFanPrompt(String fanPrompt) { this.fanPrompt = fanPrompt; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
}
