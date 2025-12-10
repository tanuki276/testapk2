package com.tci.injector;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tci.injector.R;

public class ScriptExecutorActivity extends Activity {
    
    private TextView consoleOutput;
    private Button selectScriptButton;
    private Button executeScriptButton;
    private TextView scriptPathDisplay;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_executor);

        consoleOutput = findViewById(R.id.script_console_output);
        scriptPathDisplay = findViewById(R.id.script_path_display);
        selectScriptButton = findViewById(R.id.select_script_button);
        executeScriptButton = findViewById(R.id.execute_script_button);

        appendConsoleLine("[TCI Console Log]");
        appendConsoleLine("> Core Engine Initialized. Secure Sandbox Mode.");

        selectScriptButton.setOnClickListener(this::selectScriptFile);
        executeScriptButton.setOnClickListener(this::executeScript);
        
        executeScriptButton.setEnabled(false);
    }
    
    private void selectScriptFile(View view) {
        String fakePath = "/storage/emulated/0/TCI/scripts/Titan_MAX.js";
        scriptPathDisplay.setText("Selected File: " + fakePath);
        executeScriptButton.setEnabled(true);
        appendConsoleLine("> [LOAD] Script loaded from " + fakePath);
        Toast.makeText(this, "TCI: Script Module Ready.", Toast.LENGTH_SHORT).show();
    }

    private void executeScript(View view) {
        if (!executeScriptButton.isEnabled()) {
            Toast.makeText(this, "ERROR: No script loaded. Select file first.", Toast.LENGTH_SHORT).show();
            return;
        }

        executeScriptButton.setEnabled(false);
        selectScriptButton.setEnabled(false);
        
        appendConsoleLine("\n> [EXEC] Analyzing script integrity...");
        
        String[] scriptLines = {
            "TCI.hook('Critical function initiated...');",
            "TCI.inject('0x7F1A4BC0', 99999999);",
            "TCI.log('Injection successful. System ready.');"
        };
        
        for (int i = 0; i < scriptLines.length; i++) {
            final String line = scriptLines[i];
            handler.postDelayed(() -> appendConsoleLine(line), 800 * (i + 1));
        }

        handler.postDelayed(() -> {
            appendConsoleLine("\n> [COMPLETE] Script execution finished.");
            executeScriptButton.setEnabled(true);
            selectScriptButton.setEnabled(true);
            Toast.makeText(this, "TCI: Injection Completed.", Toast.LENGTH_LONG).show();
        }, 800 * (scriptLines.length + 1));
    }
    
    private void appendConsoleLine(String line) {
        String currentText = consoleOutput.getText().toString();
        consoleOutput.setText(currentText + "\n" + line);
    }
}
