package com.thalmic.android.sample.helloworld;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;
import android.widget.Toast;
import com.canvas.AssetInstaller;
import com.canvas.LipiTKJNIInterface;
import com.canvas.LipitkResult;
import com.canvas.Stroke;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Composition {
    private static Context mContext;
    private static LipiTKJNIInterface _recognizer;

    private String draftMessage;
    private boolean currentlyDrawing = false;
    private Stroke currentStroke;
    private ArrayList<Stroke> strokes;

    public Composition() {
        draftMessage = "";
    }

    public static void init(Context context) {
        mContext = context;
        // Install required assets for LipiTk recognition
        AssetInstaller assetInstaller = new AssetInstaller(mContext, "projects");
        try {
            assetInstaller.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize lipitk
        File externalFileDir = mContext.getExternalFilesDir(null);
        String path = externalFileDir.getPath();
        Log.d("JNI", "Path: " + path);
        _recognizer = new LipiTKJNIInterface(path, "SHAPEREC_ALPHANUM");
        _recognizer.initialize();
    }

    public void addPoint(int x, int y) {
        if (currentStroke == null) {
            currentStroke = new Stroke();
        }
        currentStroke.addPoint(new PointF(x, y));
    }

    public void endStroke() {
        if (currentStroke != null) {
            strokes.add(currentStroke);
            currentStroke = null;
        }
    }

    public void next() {
        draftMessage += finishLetter();
        start();
    }

    public void back() {
        if (strokes.size() > 0) {
            strokes = new ArrayList<Stroke>();
        } else if (draftMessage.length() > 0) {
            draftMessage = draftMessage.substring(0, draftMessage.length()-1);
        }
    }

    public String peek() {
        if (draftMessage.length() > 0) {
            return draftMessage.substring(draftMessage.length()-1);
        }
        return null;
    }

    public String finish() {
        draftMessage += finishLetter();
        return draftMessage;
    }

    public void start() {
        currentlyDrawing = true;
        strokes = new ArrayList<Stroke>();
    }

    private String finishLetter() {
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

        Toast.makeText(mContext, character, Toast.LENGTH_SHORT).show();

        return character;
    }
}
