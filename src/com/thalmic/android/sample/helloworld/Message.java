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
        String[] responses = Language.getResponses(mText);

        for (int i=0; i<HelloWorldActivity.MAX_LIST_ITEMS - 1; i++) {
            String body;
            if (i<responses.length) {
                body = responses[i];
            } else {
                body = " ";
            }
            list.add(createMessageResponse(body));
        }
        list.add(createMessageResponse("Compose..."));

        return list;
    }

    public MessageResponse createMessageResponse(String text) {
        return new MessageResponse(mFrom, text);
    }
}
