package com.tci.injector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tci.injector.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ScriptExecutorActivity extends Activity {

    private static final int OPEN_JS = 4201;
    private TextView consoleOutput;
    private TextView scriptPathDisplay;
    private EditText scriptEditor;
    private WebView jsEngine;
    private Button executeScriptButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_executor);

        consoleOutput = findViewById(R.id.script_console_output);
        scriptPathDisplay = findViewById(R.id.script_path_display);
        scriptEditor = findViewById(R.id.script_editor);
        Button selectScriptButton = findViewById(R.id.select_script_button);
        executeScriptButton = findViewById(R.id.execute_script_button);

        jsEngine = new WebView(this);
        WebSettings settings = jsEngine.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setAllowFileAccessFromFileURLs(false);
        jsEngine.addJavascriptInterface(new SandboxBridge(), "Titan");
        jsEngine.loadDataWithBaseURL("https://local.titan.invalid/", "<html><body></body></html>", "text/html", "UTF-8", null);

        appendConsoleLine("[TITAN SCRIPT LAB]");
        appendConsoleLine("> Local JavaScript engine initialized.");
        appendConsoleLine("> H5GG-style demo API: log / search / getValue / setValue / freeze");
        appendConsoleLine("> SAFETY: API calls operate on synthetic data only.");

        selectScriptButton.setOnClickListener(this::selectScriptFile);
        executeScriptButton.setOnClickListener(this::executeScript);
    }

    private void selectScriptFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/javascript");
        startActivityForResult(intent, OPEN_JS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != OPEN_JS || resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        try {
            String source = readText(uri);
            scriptEditor.setText(source);
            scriptPathDisplay.setText("SCRIPT  ·  " + uri.toString());
            appendConsoleLine("> [LOAD] Local JS file loaded: " + uri.getLastPathSegment());
            appendConsoleLine("> [SAFE] Loaded source stays inside this app's sandbox.");
            Toast.makeText(this, "JavaScript loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendConsoleLine("> [ERROR] Could not read JS file: " + e.getMessage());
            Toast.makeText(this, "Could not read JavaScript file", Toast.LENGTH_SHORT).show();
        }
    }

    private String readText(Uri uri) throws Exception {
        StringBuilder result = new StringBuilder();
        try (InputStream input = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append('\n');
        }
        return result.toString();
    }

    private void executeScript(View view) {
        final String source = scriptEditor.getText().toString().trim();
        if (source.isEmpty()) {
            Toast.makeText(this, "Load a .js file or enter JavaScript first", Toast.LENGTH_SHORT).show();
            return;
        }

        executeScriptButton.setEnabled(false);
        appendConsoleLine("\n> [EXEC] Running JavaScript locally...");

        String encoded = android.util.Base64.encodeToString(source.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
        String js = "(function(){try{" +
                "var src=atob('" + encoded + "');" +
                "var module={exports:{}};" +
                "var TitanAPI={log:function(x){Titan.log(String(x));}," +
                "search:function(v,t){return Titan.search(String(v),String(t));}," +
                "getValue:function(a){return Titan.getValue(String(a));}," +
                "setValue:function(a,v){return Titan.setValue(String(a),v);}," +
                "freeze:function(a){return Titan.freeze(String(a));}};" +
                "var fn=new Function('Titan','h5gg','module','exports',src);" +
                "fn(TitanAPI,TitanAPI,module,module.exports);" +
                "Titan.log('[DONE] JavaScript execution completed locally.');" +
                "}catch(e){Titan.log('[JS ERROR] '+e.name+': '+e.message);}" +
                "})();";
        jsEngine.evaluateJavascript(js, value -> {
            executeScriptButton.setEnabled(true);
        });
    }

    private void appendConsoleLine(String line) {
        String currentText = consoleOutput.getText().toString();
        consoleOutput.setText(currentText + "\n" + line);
    }

    private class SandboxBridge {
        @JavascriptInterface public void log(String message) {
            runOnUiThread(() -> appendConsoleLine("[LOG] " + message));
        }

        @JavascriptInterface public String search(String value, String type) {
            runOnUiThread(() -> appendConsoleLine("[SEARCH] " + value + " / " + type + " → 24 synthetic results"));
            return "24";
        }

        @JavascriptInterface public String getValue(String address) {
            runOnUiThread(() -> appendConsoleLine("[GET] " + address + " → synthetic value 100"));
            return "100";
        }

        @JavascriptInterface public boolean setValue(String address, Object value) {
            runOnUiThread(() -> appendConsoleLine("[SET] " + address + " → synthetic value " + value));
            return true;
        }

        @JavascriptInterface public boolean freeze(String address) {
            runOnUiThread(() -> appendConsoleLine("[FREEZE] " + address + " marked frozen in simulator"));
            return true;
        }
    }
}
