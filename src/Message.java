package com.bookstore;

public class Message {
    private int id;
    private String senderLogin;
    private String receiverLogin;
    private String text;
    private String timestamp;

    public Message(int id, String senderLogin, String receiverLogin, String text, String timestamp) {
        this.id = id;
        this.senderLogin = senderLogin;
        this.receiverLogin = receiverLogin;
        this.text = text;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public String getSenderLogin() {
        return senderLogin;
    }

    public String getReceiverLogin() {
        return receiverLogin;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }
}