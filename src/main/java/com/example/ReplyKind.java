package com.example;

/** Whether a reply comes from a fan or an anti-fan. */
public enum ReplyKind {
    FAN, ANTI;

    public String json() {
        return name().toLowerCase();
    }
}
