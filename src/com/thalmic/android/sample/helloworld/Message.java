package com.thalmic.android.sample.helloworld;

public class Message {
    protected final String mText;
    protected final String mFrom;

    public Message(String from, String text) {
        mFrom = from;
        mText = text;
    }

    public String toString() {
        return mText;
    }

    public MessageResponses getResponses() {
        MessageResponses list = new MessageResponses();

        for (int i=0; i<5; i++) {
            list.add(createMessageResponse(Double.toString(Math.random())));
        }

        return list;
    }

    public MessageResponse createMessageResponse(String text) {
        return new MessageResponse(mFrom, text);
    }
}
