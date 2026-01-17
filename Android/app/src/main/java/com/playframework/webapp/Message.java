package com.playframework.webapp;

public class Message {
    private final int id;
    private final User author;
    private final String text;
    private final String timestamp;

    public Message(int id, User author, String text, String timestamp) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
