package com.example;

/** The language and style fans reply in. */
public enum FanVoice {
    ENGLISH, KOREAN, MIXED;

    public static FanVoice parse(String value) {
        return valueOf(value.trim().toUpperCase());
    }

    public String json() {
        return name().toLowerCase();
    }
}
