package com.tci.injector;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import android.util.Log;

public class TCIBridge {
    
    private Context context;
    private static final String TAG = "TCI_JS_ENGINE";

    public TCIBridge(Context c) {
        this.context = c;
    }

    @JavascriptInterface
    public void log(String message) {
        Log.d(TAG, "[JS_LOG] " + message);
    }

    @JavascriptInterface
    public void showToast(String message) {
        Toast.makeText(context, 
            "TCI STATUS: " + message, 
            Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void setGameValue(String targetAddress, int newValue) {
        Toast.makeText(context, 
            "INJECT SUCCESS: Address " + targetAddress + " overwritten with " + newValue, 
            Toast.LENGTH_LONG).show();
    }
}
