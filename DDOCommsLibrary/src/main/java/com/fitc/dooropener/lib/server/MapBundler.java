package com.fitc.dooropener.lib.server;

import android.os.Bundle;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jon on 14/10/2015.
 */
public class MapBundler {


    public static Bundle toBundle(Map<String, String> input) {
        Bundle output = new Bundle();
        for(String key : input.keySet()) {
            output.putString(key, input.get(key));
        }
        return output;
    }

    public static Map<String, String> fromBundle(Bundle input) {
        Map<String, String> output = new HashMap<String, String>();
        for(String key : input.keySet()) {
            output.put(key, input.getString(key));
        }
        return output;
    }
}
