package com.thalmic.android.sample.helloworld;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.getpebble.android.kit.util.PebbleDictionary;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class ApiBrowser {
    private final Context mContext;
    private final VolleyQueue volleyQueue;
    private Pebble mPebble;

    private final String TAG = "ApiBrowser";

    private LinkedHashMap<String, String> apis = new LinkedHashMap<String, String>();

    public ApiBrowser(Context context, Pebble pebble) {
        mContext = context;
        mPebble = pebble;
        volleyQueue = VolleyQueue.getInstance(context);

        apis.put("Top Movies", "http://api.rottentomatoes.com/api/public/v1.0/lists/movies/box_office.json?apikey=gggx6ge2d2gn9baf9kdgh9cw&limit=5");
    }

    private String getApiByIndex(int index) {
        return (String) apis.values().toArray()[index];
    }

    public void launch() {
        volleyQueue.getRequestQueue().cancelAll(TAG);

        PebbleDictionary data = new PebbleDictionary();
        int i=1;
        for (String key : apis.keySet()) {
            data.addString(i, key);
            i++;
        }
        mPebble.send(data);
    }

    public void select(int index) {
        String url = getApiByIndex(index);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener() {
                    @Override
                    public void onResponse(Object obj) {
                        JSONObject json = (JSONObject) obj;

                        PebbleDictionary data = new PebbleDictionary();
                        int i=1;
                        for (String key : apis.values()) {
                            data.addString(i, key);
                            i++;
                        }
                        mPebble.send(data);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError e) {
                        e.printStackTrace();
                    }
                });
        jsObjRequest.setTag(TAG);

        volleyQueue.addToRequestQueue(jsObjRequest);

    }
}
