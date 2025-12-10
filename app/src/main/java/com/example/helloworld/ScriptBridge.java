package com.ghosthacker.app;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class ScriptBridge {
    private Context context;

    public ScriptBridge(Context c) {
        this.context = c;
    }

    @JavascriptInterface
    public void log(String message) {
        
    }

    @JavascriptInterface
    public void showToast(String message) {
        Toast.makeText(context, "JS CALL: " + message, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void setGameValue(String targetAddress, int newValue) {
        Toast.makeText(context, 
            "Native Hook: " + targetAddress + " <-- " + newValue + " (SUCCESS)", 
            Toast.LENGTH_LONG).show();
    }
}
