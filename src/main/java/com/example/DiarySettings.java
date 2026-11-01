package com.example;

/** A user's diary settings. In update requests, null fields are left unchanged and "" clears optional text. */
public record DiarySettings(
        String displayName,
        String fandom,
        String avatar,
        String statusText,
        String profileMessage,
        String voice,
        String fanPrompt) {

    public static DiarySettings of(User user) {
        return new DiarySettings(user.getDisplayName(), user.getFandom(), user.getAvatar(), user.getStatusText(),
                user.getProfileMessage(), user.getVoice().json(), user.getFanPrompt());
    }
}
