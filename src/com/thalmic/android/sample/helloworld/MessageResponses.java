package com.thalmic.android.sample.helloworld;

import java.util.ArrayList;
import java.util.List;

public class MessageResponses extends ArrayList<MessageResponse> {
    public List<String> getStringList() {
        List<String> list = new ArrayList<String>();
        for (MessageResponse m : this) {
            list.add(m.toString());
        }
        return list;
    }
}
