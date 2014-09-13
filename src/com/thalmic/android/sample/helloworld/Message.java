package com.thalmic.android.sample.helloworld;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private final String mText;

    public Message(String text) {
        mText = text;
    }

    public String toString() {
        return mText;
    }

    public MessageResponses getResponses() {
        MessageResponses list = new MessageResponses();

        for (int i=0; i<5; i++) {
            list.add(new Message(Double.toString(Math.random())));
        }

        return list;
    }
}
