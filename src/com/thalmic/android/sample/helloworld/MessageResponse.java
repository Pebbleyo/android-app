package com.thalmic.android.sample.helloworld;

public class MessageResponse extends Message {

    public MessageResponse(String from, String text) {
        super(from, text);
    }

    @Override
    public MessageResponses getResponses() {
        return null;
    }

    public boolean send() {
        return true;
    }
}
