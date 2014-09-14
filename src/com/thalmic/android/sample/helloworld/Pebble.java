package com.thalmic.android.sample.helloworld;

import android.content.Context;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

public class Pebble {
    public final static UUID PEBBLE_APP_UUID = UUID.fromString("1d139f51-14b0-4a9e-882e-91df056ff7fe");
    private static final int KEY_DISPLAY_MESSAGE = 6;
    private final Context mContext;

    public Pebble(Context context) {
        mContext = context;

        PebbleKit.registerReceivedAckHandler(mContext, new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                Log.i("Pebble", "Received ack for transaction " + transactionId);
            }
        });

        PebbleKit.registerReceivedNackHandler(mContext, new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                Log.i("Pebble", "Received nack for transaction " + transactionId);
            }
        });
    }

    public boolean isConnected() {
        return PebbleKit.isWatchConnected(mContext);
    }

    public void startApp() {
        PebbleKit.startAppOnPebble(mContext, PEBBLE_APP_UUID);
    }

    public void displayMessage(Message message) {
        PebbleDictionary data = new PebbleDictionary();
        data.addString(KEY_DISPLAY_MESSAGE, message.toString());
        send(data);
    }

    public void displayResponses(MessageResponses messageResponses) {
        PebbleDictionary data = new PebbleDictionary();
        for (int i=0; i<messageResponses.size(); i++) {
            data.addString(i, messageResponses.get(i).toString());
            send(data);
        }
    }

    private void send(PebbleDictionary data) {
        PebbleKit.sendDataToPebble(mContext, PEBBLE_APP_UUID, data);
    }
}
