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

    public List<String> getResponses() {
        List<String> list = new ArrayList<String>();

        for (int i=0; i<5; i++) {
            list.add(Double.toString(Math.random()));
        }

        return list;
    }
}
