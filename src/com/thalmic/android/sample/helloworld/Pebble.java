package com.thalmic.android.sample.helloworld;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

public class Pebble {
    public final static UUID PEBBLE_APP_UUID = UUID.fromString("1d139f51-14b0-4a9e-882e-91df056ff7fe");
    private static final int KEY_SET_INDEX = 6;
    private static final int KEY_DISPLAY_MESSAGE = 7;
    private static final int KEY_SHOW_LIST = 8;
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
            data.addString(i+1, messageResponses.get(i).toString());
        }
        send(data);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                PebbleDictionary data2 = new PebbleDictionary();
                data2.addInt8(KEY_SHOW_LIST, (byte) 42);
                send(data2);
            }
        }, 500);
    }

    public void setIndex(int index) {
        PebbleDictionary data = new PebbleDictionary();
        data.addUint16(KEY_SET_INDEX, (short) index);
        send(data);
    }

    public void send(PebbleDictionary data) {
        PebbleKit.sendDataToPebble(mContext, PEBBLE_APP_UUID, data);
    }

    public void closeApp() {
        PebbleKit.closeAppOnPebble(mContext, PEBBLE_APP_UUID);
    }
}
