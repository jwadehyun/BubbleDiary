package com.example;

/** A user's diary settings. In update requests, null fields are left unchanged. */
public record DiarySettings(
        String displayName,
        String fandom,
        String avatar,
        String model,
        String voice,
        Integer replyCount,
        Integer fillerCount) {

    public static DiarySettings of(User user) {
        return new DiarySettings(user.getDisplayName(), user.getFandom(), user.getAvatar(), user.getModel(),
                user.getVoice().json(), user.getReplyCount(), user.getFillerCount());
    }
}
