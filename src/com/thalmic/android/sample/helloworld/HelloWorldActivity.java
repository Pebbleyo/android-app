
/*
* Copyright (C) 2014 Thalmic Labs Inc.
* Distributed under the Myo SDK license agreement. See LICENSE.txt for details.
*/
package com.thalmic.android.sample.helloworld;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
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
import com.canvas.LipitkResult;
import com.canvas.Stroke;
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
import java.util.ArrayList;
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
    private int state;

    private static final int REQUEST_ENABLE_BT = 1;

    private final static UUID PEBBLE_APP_UUID = UUID.fromString("1d139f51-14b0-4a9e-882e-91df056ff7fe");

    private Queue<Message> messageQueue = new LinkedList<Message>();
    private Message currentMessage;
    private boolean currentlyViewingMessage = false;

    private String draftMessage = "";

    private TextView mTextView;
    private Float baseScrollPitch;
    private float roll;
    private float pitch;
    private float yaw;

    private boolean currentlyDrawing = false;
    private Float baseDrawPitch;
    private Float baseDrawYaw;

    private Pose currentPose;
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

            switch (state) {
                case STATE_READY:
                    break;
                case STATE_MESSAGE_RECEIVED_UNREAD:
                    if (roll < 10 && roll > -10 && pitch > 0) {
                        state = STATE_MESSAGE_RECEIVED_READING;
                    }
                    break;
                case STATE_MESSAGE_RECEIVED_READING:
                    break;
                case STATE_RESPONDING:
                    if (baseScrollPitch == null) baseScrollPitch = pitch;
                    mListView.setItemChecked((int) ((pitch - baseScrollPitch) / 5), true);
                    break;
                case STATE_COMPOSING:
                    if (baseDrawPitch == null) baseDrawPitch = pitch;
                    if (baseDrawYaw == null) baseDrawYaw = yaw;

                    int x = 150 - (int) (Math.sin((yaw - baseDrawYaw)     * Math.PI / 180) * 200);
                    int y = 150 + (int) (Math.sin((pitch - baseDrawPitch) * Math.PI / 180) * 200);

                    mCircleView.setCircleLocation(x, y);

                    if (currentPose == Pose.FIST) {
                        if (currentStroke == null) {
                            currentStroke = new Stroke();
                        }
                        currentStroke.addPoint(new PointF(x, y));
                    } else {
                        if (currentStroke != null) {
                            strokes.add(currentStroke);
                            currentStroke = null;
                        }
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

            if (currentPose == pose) return;
            else currentPose = pose;

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

                    if (state == STATE_MESSAGE_RECEIVED_READING) {
                        setState(STATE_RESPONDING);
                    } else if (state == STATE_COMPOSING) {
                        finishComposition();
                    }

                    break;
                case WAVE_IN:
                    mTextView.setText("Pose at wave in");

                    if (state == STATE_COMPOSING) {
                        if (mArm == Arm.LEFT) {
                            nextLetter();
                        } else {
                            backspace();
                        }
                    }

                    break;
                case WAVE_OUT:
                    mTextView.setText("Pose at wave out");

                    if (state == STATE_COMPOSING) {
                        if (mArm == Arm.RIGHT) {
                            nextLetter();
                        } else {
                            backspace();
                        }
                    }

                    break;
                case FINGERS_SPREAD:
                    mTextView.setText("Pose at fingers spread");

                    if (state == STATE_MESSAGE_RECEIVED_READING) {
                        setState(STATE_READY);
                    }

                    break;
                case THUMB_TO_PINKY:
                    mTextView.setText("Pose at thumb to pinky");
                    break;
            }
        }
    };

    private boolean setState(int newState) {
        if (state == newState) return false;

        switch (newState) {
            case STATE_READY:
                clearMessageResponses();
                break;
            case STATE_RESPONDING:
                displayMessageResponses(currentMessage);
            case STATE_COMPOSING:
                startDrawing();
                break;
        }

        state = newState;
        return true;
    }

    private TextView mTimeTextView;
    private ListView mListView;
    private CircleView mCircleView;
    private LipiTKJNIInterface _recognizer;
    private Stroke currentStroke;
    private ArrayList<Stroke> strokes;
    private Pebble pebble;

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

        pebble = new Pebble(this);
        boolean connected = pebble.isConnected();
        Log.i(getLocalClassName(), "Pebble is " + (connected ? "connected" : "not connected"));

        if (connected) {
            pebble.startApp();
            Log.i(getLocalClassName(), "Starting app with UUID " + PEBBLE_APP_UUID.toString());
        }

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
        _recognizer = new LipiTKJNIInterface(path, "SHAPEREC_ALPHANUM");
        _recognizer.initialize();
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

    private void newComposition() {
        draftMessage = "";
        startDrawing();
    }

    private void nextLetter() {
        draftMessage += stopDrawing();
        startDrawing();
    }

    private void backspace() {
        if (strokes.size() > 0) {
            strokes = new ArrayList<Stroke>();
        } else {
            draftMessage = draftMessage.substring(0, draftMessage.length()-1);
        }
    }

    private void finishComposition() {
        draftMessage += stopDrawing();
        Toast.makeText(this, draftMessage, Toast.LENGTH_SHORT).show();
    }

    private void startDrawing() {
        currentlyDrawing = true;
        strokes = new ArrayList<Stroke>();
    }

    private String stopDrawing() {
        currentlyDrawing = false;
        String character;

        if (strokes.size() > 0) {
            Stroke[] strokesArray = new Stroke[strokes.size()];
            for (int s = 0; s < strokes.size(); s++)
                strokesArray[s] = strokes.get(s);

            LipitkResult[] results = _recognizer.recognize(strokesArray);

            for (LipitkResult result : results) {
                Log.e("jni", "ShapeID = " + result.Id + " Confidence = " + result.Confidence);
            }

            String configFileDirectory = _recognizer.getLipiDirectory() + "/projects/alphanumeric/config/";
            //        character=new String[results.length];
            //        for(int i=0;i<character.length;i++){
            //            character[i] = _recognizer.getSymbolName(results[i].Id, configFileDirectory);
            //        }

            character = _recognizer.getSymbolName(results[0].Id, configFileDirectory);
        } else {
            character = " ";
        }

        Toast.makeText(this, character, Toast.LENGTH_SHORT).show();

        return character;
    }
}


