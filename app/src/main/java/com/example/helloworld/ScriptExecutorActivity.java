package com.tci.injector;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tci.injector.R;

import java.util.Locale;

public class ScriptExecutorActivity extends Activity {

    private TextView consoleOutput;
    private TextView scriptPathDisplay;
    private EditText scriptEditor;
    private Button executeScriptButton;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_executor);

        consoleOutput = findViewById(R.id.script_console_output);
        scriptPathDisplay = findViewById(R.id.script_path_display);
        scriptEditor = findViewById(R.id.script_editor);
        Button selectScriptButton = findViewById(R.id.select_script_button);
        executeScriptButton = findViewById(R.id.execute_script_button);

        appendConsoleLine("[TITAN SCRIPT LAB]");
        appendConsoleLine("> Local sandbox initialized.");
        appendConsoleLine("> H5GG-style API surface: log / search / get / set / freeze");
        appendConsoleLine("> SAFETY: all memory calls use synthetic data only.");

        selectScriptButton.setOnClickListener(this::selectScriptFile);
        executeScriptButton.setOnClickListener(this::executeScript);
    }

    private void selectScriptFile(View view) {
        String fakePath = "/storage/emulated/0/TitanCore/scripts/demo.js";
        scriptPathDisplay.setText("SCRIPT  ·  " + fakePath);
        scriptEditor.setText("log('Titan Core demo');\nsearch('100', 'I32');\ngetValue('0x1000');\nsetValue('0x1000', 200);\nfreeze('0x1000');");
        appendConsoleLine("> [LOAD] demo.js selected.");
        appendConsoleLine("> [SAFE] Script is executed against the local simulator.");
        Toast.makeText(this, "Demo script loaded", Toast.LENGTH_SHORT).show();
    }

    private void executeScript(View view) {
        final String source = scriptEditor.getText().toString().trim();
        if (source.isEmpty()) {
            Toast.makeText(this, "Enter a script or load the demo", Toast.LENGTH_SHORT).show();
            return;
        }

        executeScriptButton.setEnabled(false);
        appendConsoleLine("\n> [DRY-RUN] Parsing script...");

        String[] lines = source.split("\\r?\\n");
        int delay = 350;
        for (String line : lines) {
            final String command = line.trim();
            if (command.isEmpty()) continue;
            final String result = simulateCommand(command);
            handler.postDelayed(() -> appendConsoleLine(result), delay);
            delay += 450;
        }

        final int finishDelay = delay + 250;
        handler.postDelayed(() -> {
            appendConsoleLine("> [DONE] Dry-run completed. No external process was touched.");
            executeScriptButton.setEnabled(true);
            Toast.makeText(this, "Dry-run completed", Toast.LENGTH_SHORT).show();
        }, finishDelay);
    }

    private String simulateCommand(String command) {
        String lower = command.toLowerCase(Locale.US);
        if (lower.startsWith("log(")) {
            return "[LOG] " + command.substring(4).replace(";", "").trim();
        }
        if (lower.startsWith("search(")) {
            return "[SEARCH] synthetic region scan → 24 results (simulated)";
        }
        if (lower.startsWith("getvalue(")) {
            return "[GET] synthetic value → 100 (simulated)";
        }
        if (lower.startsWith("setvalue(")) {
            return "[SET] synthetic value updated locally → 200 (simulated)";
        }
        if (lower.startsWith("freeze(")) {
            return "[FREEZE] synthetic entry marked frozen (simulated)";
        }
        return "[SKIP] Unsupported demo command; no system operation performed.";
    }

    private void appendConsoleLine(String line) {
        String currentText = consoleOutput.getText().toString();
        consoleOutput.setText(currentText + "\n" + line);
    }
}
