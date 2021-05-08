package com.example.nerechat.chatbot;

public class MessageBot {

    private String message;
    private boolean isReceived;

    public MessageBot(String message, boolean isReceived) {
        this.message = message;
        this.isReceived = isReceived;

    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getIsReceived() {
        return isReceived;
    }

    public void setIsReceived(boolean isReceived) {
        this.isReceived = isReceived;
    }
}
