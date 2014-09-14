
/*
* Copyright (C) 2014 Thalmic Labs Inc.
* Distributed under the Myo SDK license agreement. See LICENSE.txt for details.
*/
package com.thalmic.android.sample.helloworld;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.thalmic.myo.*;
import com.thalmic.myo.scanner.ScanActivity;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class HelloWorldActivity extends Activity {
    // This code will be returned in onActivityResult() when the enable Bluetooth activity exits.

    private static final int STATE_READY = 0;
    private static final int STATE_MESSAGE_RECEIVED_UNREAD = 1;
    private static final int STATE_MESSAGE_RECEIVED_READING = 2;
    private static final int STATE_RESPONDING = 3;
    private static final int STATE_COMPOSING = 4;
    private static final int STATE_API_BROWSER_LIST = 5;
    private static final int STATE_API_BROWSER_RESULTS = 6;
    private int state;

    public static final int MAX_LIST_ITEMS = 5;

    private static final int REQUEST_ENABLE_BT = 1;

    private Queue<Message> messageQueue = new LinkedList<Message>();
    private Message currentMessage;
    private MessageResponses currentResponses;
    private int currentIndex;

    private TextView mTextView;
    private Float baseScrollPitch;
    private float roll;
    private float pitch;
    private float yaw;

    private Float baseDrawPitch;
    private Float baseDrawYaw;

    private Pose currentPose;
    private Pose previousPose;
    private Myo mMyo;
    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {
        private Arm mArm = Arm.UNKNOWN;
        private XDirection mXDirection = XDirection.UNKNOWN;
        public Handler poseDebounceHandler = new Handler();

        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {
            // Set the text color of the text view to cyan when a Myo connects.
            mTextView.setTextColor(Color.CYAN);
            mTextView.setText("Myo has connected");
            mMyo = myo;
        }
        // onDisconnect() is called whenever a Myo has been disconnected.
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            // Set the text color of the text view to red when a Myo disconnects.
            mTextView.setTextColor(Color.RED);
            mTextView.setText("Myo has disconnected");
        }
        // onArmRecognized() is called whenever Myo has recognized a setup gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mArm = arm;
            mXDirection = xDirection;
            mTextView.setTextColor(Color.CYAN);
            mTextView.setText("Arm recognized");
        }
        // onArmLost() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmLost(Myo myo, long timestamp) {
            mArm = Arm.UNKNOWN;
            mXDirection = XDirection.UNKNOWN;
            mTextView.setTextColor(Color.RED);
            mTextView.setText("Arm not recognized");
        }
        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));

            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (mXDirection == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }

            mTimeTextView.setText(String.format("R %.3f | P %.3f | Y %.3f", roll, pitch, yaw));

            switch (state) {
                case STATE_READY:
                    break;
                case STATE_MESSAGE_RECEIVED_UNREAD:
//                    if (roll < -10 && roll > -50 && pitch < 0) {
//                        setState(STATE_MESSAGE_RECEIVED_READING);
//                    }
                    break;
                case STATE_MESSAGE_RECEIVED_READING:
                    break;
                case STATE_RESPONDING:
                case STATE_API_BROWSER_LIST:
                case STATE_API_BROWSER_RESULTS:
                    if (baseScrollPitch == null) baseScrollPitch = pitch;

                    int newIndex = (int) ((pitch - baseScrollPitch) / MAX_LIST_ITEMS);
                    if (newIndex < 0) newIndex = 0;
                    if (newIndex >= MAX_LIST_ITEMS) newIndex = MAX_LIST_ITEMS-1;

                    if (newIndex != currentIndex) {
                        currentIndex = newIndex;
                        mListView.setItemChecked(currentIndex, true);
                        pebble.setIndex(currentIndex);
                    }

                    break;
                case STATE_COMPOSING:
                    if (baseDrawPitch == null) baseDrawPitch = pitch;
                    if (baseDrawYaw == null) baseDrawYaw = yaw;

                    int x = 150 - (int) (((yaw - baseDrawYaw)     / 180) * 200);
                    int y = 150 + (int) (((pitch - baseDrawPitch)  / 180) * 200);

                    mCircleView.setCircleLocation(x, y);

                    if (currentPose == Pose.FIST || currentPose == Pose.THUMB_TO_PINKY) {
                        composition.addPoint(x, y);
                    } else {
                        composition.endStroke();
                    }
                    break;
            }
        }


        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(final Myo myo, final long timestamp, final Pose pose) {
            poseDebounceHandler.removeCallbacksAndMessages(null);
            poseDebounceHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        onPoseHeld(myo, timestamp, pose);
                    }
                }
            , 100);
        }

        public void onPoseHeld(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.

            if (currentPose == pose) {
                return;
            } else {
                previousPose = currentPose;
                currentPose = pose;
            }

            switch (pose) {
                case UNKNOWN:
                    mTextView.setText("Pose unknown");
                    break;
                case REST:
                    int restTextId = R.string.hello_world;
                    switch (mArm) {
                        case LEFT:
                            restTextId = R.string.arm_left;
                            break;
                        case RIGHT:
                            restTextId = R.string.arm_right;
                            break;
                    }
                    if (state == STATE_COMPOSING && (previousPose == Pose.FIST || previousPose == Pose.THUMB_TO_PINKY)) {
                        vibrate();
                    }
                    mTextView.setText("Pose at rest");
                    break;
                case FIST:
                case THUMB_TO_PINKY:
                    mTextView.setText("Pose at fist / thumb to pinky");

                    if (state == STATE_MESSAGE_RECEIVED_READING) {
                        vibrate();
                        setState(STATE_RESPONDING);
                    } else if (state == STATE_RESPONDING) {
                        vibrate();
                        if (currentIndex != MAX_LIST_ITEMS - 1) {
                            send(currentResponses.get(currentIndex));
                        } else {
                            setState(STATE_COMPOSING);
                        }
                    } else if (state == STATE_COMPOSING) {
                        vibrate();
                    } else if (state == STATE_API_BROWSER_LIST) {
                        vibrate();
                        apiBrowser.select(currentIndex);
                        setState(STATE_API_BROWSER_RESULTS);
                    }

                    break;
                case WAVE_IN:
                    mTextView.setText("Pose at wave in");

                    if (state == STATE_COMPOSING) {
                        vibrate();
                        if (mArm == Arm.LEFT) {
                            compositionNextOrFinish();
                        } else {
                            composition.back();
                        }
                    }

                    break;
                case WAVE_OUT:
                    mTextView.setText("Pose at wave out");

                    if (state == STATE_COMPOSING) {
                        vibrate();
                        if (mArm == Arm.RIGHT) {
                            compositionNextOrFinish();
                        } else {
                            composition.back();
                        }
                    }

                    break;
                case FINGERS_SPREAD:
                    mTextView.setText("Pose at fingers spread");

                    if (state != STATE_READY) vibrate();

                    switch (state) {
                        case STATE_API_BROWSER_RESULTS:
                            apiBrowser.launch();
                            setState(STATE_API_BROWSER_LIST);
                            break;
                        case STATE_API_BROWSER_LIST:
                        case STATE_MESSAGE_RECEIVED_READING:
                        case STATE_COMPOSING:
                        case STATE_RESPONDING:
                            setState(STATE_READY);
                            break;
                    }

                    break;
            }
        }
    };
    private Composition composition;
    private TextView mMessageView;
    public FbChat fbChat;
    private ApiBrowser apiBrowser;

    private void compositionNextOrFinish() {
        String prevChar = composition.peek();
        if (prevChar != null && prevChar.equals(" ")) {
            if (currentMessage != null) send(currentMessage.createMessageResponse(composition.finish()));
        } else {
            composition.next();
        }
    }

    private boolean setState(int newState) {
        if (state == newState) return false;

        switch (newState) {
            case STATE_READY:
                clearMessageResponses();
                if (composition != null) composition.finish();
                currentMessage = null;
                currentIndex = -1;
                baseScrollPitch = null;
                baseDrawPitch = null;
                baseDrawYaw = null;
                break;
            case STATE_RESPONDING:
                displayMessageResponses(currentMessage);
                break;
            case STATE_COMPOSING:
                composition = new Composition();
                composition.start();
                break;
        }

        state = newState;

        Log.i(getLocalClassName(), "New state : " + Integer.toString(newState));

        return true;
    }

    private TextView mTimeTextView;
    private ListView mListView;
    private CircleView mCircleView;
    private Pebble pebble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_world);
        mTextView = (TextView) findViewById(R.id.text);
        mTimeTextView = (TextView) findViewById(R.id.timestamp);
        mMessageView = (TextView) findViewById(R.id.messageView);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mCircleView = (CircleView) findViewById(R.id.circle_view);
        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);

        pebble = new Pebble(this);
        boolean connected = pebble.isConnected();
        Log.i(getLocalClassName(), "Pebble is " + (connected ? "connected" : "not connected"));

        if (connected) {
            pebble.startApp();
            Log.i(getLocalClassName(), "Starting app with UUID " + Pebble.PEBBLE_APP_UUID.toString());
        }

        Composition.init(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            FbChat.init(this, new FbChat.FbMessageHandler() {
                @Override
                public void onMessage(Message message) {
                    onNewMessage(message);
                }
            });
        } catch (Exception e) {
            Log.e("FbChat", "exception", e);
        }

        apiBrowser = new ApiBrowser(this, pebble);

        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(Pebble.PEBBLE_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
//                Log.i("Pebble", "Received value=" + data.getInteger(11) + " for key: 11");

                if (data.getInteger(11) != null) {
                    if (state == STATE_MESSAGE_RECEIVED_UNREAD) {
                        vibrate();
                        setState(STATE_MESSAGE_RECEIVED_READING);
                    } else if (state == STATE_READY) {
                        apiBrowser.launch();
                        setState(STATE_API_BROWSER_LIST);
                    }
                }

                PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        // If Bluetooth is not enabled, request to turn it on.
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);
        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
            Hub.getInstance().shutdown();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth, so exit.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_scan:
                onScanActionSelected();
                return true;
            case R.id.message:
                onNewMessage(new Message("Someone", String.format("Test %d", System.currentTimeMillis())));
                return true;
            case R.id.startDraw:
                setState(STATE_COMPOSING);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    private void onNewMessage(Message message) {
        messageQueue.add(message);
        if (state == STATE_READY || state == STATE_MESSAGE_RECEIVED_UNREAD) {
            currentMessage = messageQueue.poll();
            display(currentMessage);
            setState(STATE_MESSAGE_RECEIVED_UNREAD);
        }
    }

    private void display(final Message message) {
        mMessageView.setText(message.toString());
        pebble.startApp();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                pebble.displayMessage(message);
                vibrate();
            }
        }, 500);
    }

    private void send(MessageResponse message) {
        Toast.makeText(this, message.toString(), Toast.LENGTH_SHORT).show();
        message.send();
        setState(STATE_READY);
    }

    private void displayMessageResponses(Message message) {
        currentResponses = message.getResponses();
        mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, currentResponses.getStringList()));
        pebble.displayResponses(currentResponses);
    }

    private void clearMessageResponses() {
        mListView.setAdapter(null);
        pebble.closeApp();
        // TODO: go to next message in the queue
    }

    private void vibrate() {
        mMyo.vibrate(Myo.VibrationType.SHORT);
    }
}
