
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.canvas.AssetInstaller;
import com.canvas.LipiTKJNIInterface;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class HelloWorldActivity extends Activity {
    // This code will be returned in onActivityResult() when the enable Bluetooth activity exits.

    private static final int REQUEST_ENABLE_BT = 1;

    private final static UUID PEBBLE_APP_UUID = UUID.fromString("1d139f51-14b0-4a9e-882e-91df056ff7fe");

    private Queue<Message> messageQueue = new LinkedList<Message>();
    private Message currentMessage;
    private boolean currentlyViewingMessage = false;

    private TextView mTextView;
    private float lastScrollPitch;
    private float roll;
    private float pitch;
    private float yaw;

    private boolean currentlyDrawing;
    private float lastDrawPitch;
    private float lastDrawYaw;

    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {
        private Arm mArm = Arm.UNKNOWN;
        private XDirection mXDirection = XDirection.UNKNOWN;
        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {
            // Set the text color of the text view to cyan when a Myo connects.
            mTextView.setTextColor(Color.CYAN);
            mTextView.setText("Myo has connected");
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
            // Next, we apply a rotation to the text view using the roll, pitch, and yaw.
//            mTextView.setRotation(roll);
//            mTextView.setRotationX(pitch);
//            mTextView.setRotationY(yaw);

            if (!currentlyViewingMessage) {
                lastScrollPitch = pitch;
            } else {
                mListView.setItemChecked((int) ((pitch - lastScrollPitch) / 5), true);
            }

            if (!currentlyDrawing) {
                lastDrawPitch = pitch;
                lastDrawYaw = yaw;
            } else {
                mCircleView.setCircleLocation(150 - (int) (Math.sin((yaw - lastDrawYaw)     * Math.PI / 180) * 200),
                                              150 + (int) (Math.sin((pitch - lastDrawPitch) * Math.PI / 180) * 200));
            }
        }
        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.

//            PebbleDictionary data = new PebbleDictionary();
////            data.addInt8(0, (byte) pose.ordinal());
//            data.addString(0, "a");
//            PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);

//            mTimeTextView.setText(Long.toString(timestamp));

            currentlyDrawing = (pose == Pose.FIST);

            PebbleDictionary data = new PebbleDictionary();

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
                    mTextView.setText("Pose at rest");
                    break;
                case FIST:
                    mTextView.setText("Pose at fist");

                    if (!currentlyViewingMessage && currentMessage != null) {
                        displayMessageResponses(currentMessage);
                    }



                    break;
                case WAVE_IN:
                    mTextView.setText("Pose at wave in");

                    data.addInt8(11, (byte) 3);
                    PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);

                    break;
                case WAVE_OUT:
                    mTextView.setText("Pose at wave out");

                    data.addInt8(11, (byte) 2);
                    PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);

                    break;
                case FINGERS_SPREAD:
                    mTextView.setText("Pose at fingers spread");

                    if (currentlyViewingMessage) {
                        clearMessageResponses();
                    }
                    break;
                case THUMB_TO_PINKY:
                    mTextView.setText("Pose at thumb to pinky");
                    break;
            }
        }
    };
    private TextView mTimeTextView;
    private ListView mListView;
    private CircleView mCircleView;
    private LipiTKJNIInterface _lipitkInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_world);
        mTextView = (TextView) findViewById(R.id.text);
        mTimeTextView = (TextView) findViewById(R.id.timestamp);
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

        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i(getLocalClassName(), "Pebble is " + (connected ? "connected" : "not connected"));

        if (connected) {
            PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
            Log.i(getLocalClassName(), "Starting app with UUID " + PEBBLE_APP_UUID.toString());
        }

        PebbleKit.registerReceivedAckHandler(getApplicationContext(), new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                Log.i(getLocalClassName(), "Received ack for transaction " + transactionId);
            }
        });

        PebbleKit.registerReceivedNackHandler(getApplicationContext(), new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                Log.i(getLocalClassName(), "Received nack for transaction " + transactionId);
            }
        });

        // Install required assets for LipiTk recognition
        AssetInstaller assetInstaller = new AssetInstaller(getApplicationContext(), "projects");
        try {
            assetInstaller.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize lipitk
        File externalFileDir = getApplicationContext().getExternalFilesDir(null);
        String path = externalFileDir.getPath();
        Log.d("JNI", "Path: " + path);
        _lipitkInterface = new LipiTKJNIInterface(path, "SHAPEREC_ALPHANUM");
        _lipitkInterface.initialize();
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
                onNewMessage(new Message(String.format("Test %d", System.currentTimeMillis())));
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
        if (!currentlyViewingMessage) {
            currentMessage = messageQueue.poll();
            display(currentMessage);
        }
    }

    private void display(Message message) {
        mTimeTextView.setText(message.toString());
    }

    private void displayMessageResponses(Message message) {
        currentlyViewingMessage = true;
        mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, message.getResponses()));
    }

    private void clearMessageResponses() {
        currentlyViewingMessage = false;
        mListView.setAdapter(null);

        // TODO: go to next message in the queue
    }
}


