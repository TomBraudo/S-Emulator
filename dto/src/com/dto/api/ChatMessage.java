package com.dto.api;

public class ChatMessage {
    private final String username;
    private final String content;
    private final long timestamp;

    public ChatMessage(String username, String content, long timestamp) {
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

