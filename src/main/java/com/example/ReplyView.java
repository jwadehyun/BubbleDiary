package com.example;

public record ReplyView(Long id, String fan, String text, long time, int likes, boolean ai, String kind) {

    public static ReplyView of(Reply r) {
        return new ReplyView(r.getReplyId(), r.getFan(), r.getText(), r.getCreatedAt().toEpochMilli(), r.getLikes(),
                r.isAi(), r.getKind().json());
    }
}
