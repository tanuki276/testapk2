package com.tci.injector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tci.injector.R;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScriptExecutorActivity extends Activity {

    private static final int OPEN_LUA = 4202;
    private static final int MAX_SCRIPT_CHARS = 512 * 1024;

    private TextView consoleOutput;
    private TextView scriptPathDisplay;
    private EditText scriptEditor;
    private Button executeScriptButton;
    private final ExecutorService scriptExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_executor);

        consoleOutput = findViewById(R.id.script_console_output);
        scriptPathDisplay = findViewById(R.id.script_path_display);
        scriptEditor = findViewById(R.id.script_editor);
        Button selectScriptButton = findViewById(R.id.select_script_button);
        executeScriptButton = findViewById(R.id.execute_script_button);

        appendConsoleLine("[TITAN LUA LAB]");
        appendConsoleLine("> LuaJ sandbox initialized.");
        appendConsoleLine("> GG-style API: gg.searchNumber / refineNumber / getResults / editAll / setValues / addListItems");
        appendConsoleLine("> SAFETY: scripts can only modify FakeMemoryManager synthetic data.");

        selectScriptButton.setOnClickListener(this::selectScriptFile);
        executeScriptButton.setOnClickListener(this::executeScript);
    }

    private void selectScriptFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_LUA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != OPEN_LUA || resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        try {
            String source = readText(uri);
            scriptEditor.setText(source);
            String name = uri.getLastPathSegment() == null ? uri.toString() : uri.getLastPathSegment();
            scriptPathDisplay.setText("LUA  ·  " + name);
            appendConsoleLine("> [LOAD] Lua file loaded: " + name);
            appendConsoleLine("> [SAFE] io/os/package/luajava are disabled.");
            Toast.makeText(this, "Lua loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendConsoleLine("> [ERROR] Could not read Lua file: " + e.getMessage());
            Toast.makeText(this, "Could not read Lua file", Toast.LENGTH_SHORT).show();
        }
    }

    private String readText(Uri uri) throws Exception {
        StringBuilder result = new StringBuilder();
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("Document stream unavailable");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (result.length() + line.length() + 1 > MAX_SCRIPT_CHARS) {
                        throw new IllegalArgumentException("Script is larger than 512 KiB");
                    }
                    result.append(line).append('\n');
                }
            }
        }
        return result.toString();
    }

    private void executeScript(View view) {
        final String source = scriptEditor.getText().toString();
        if (source.trim().isEmpty()) {
            Toast.makeText(this, "Load a .lua file or enter Lua first", Toast.LENGTH_SHORT).show();
            return;
        }

        executeScriptButton.setEnabled(false);
        appendConsoleLine("\n> [EXEC] Running Lua locally...");

        scriptExecutor.execute(() -> {
            try {
                Globals globals = createSandboxGlobals();
                globals.load(source, "titan-script.lua").call();
                appendConsoleThreadSafe("[DONE] Lua execution completed locally.");
            } catch (Throwable error) {
                String message = error.getMessage();
                appendConsoleThreadSafe("[LUA ERROR] " + error.getClass().getSimpleName() + ": " + (message == null ? "unknown error" : message));
            } finally {
                runOnUiThread(() -> executeScriptButton.setEnabled(true));
            }
        });
    }

    private Globals createSandboxGlobals() {
        Globals globals = JsePlatform.standardGlobals();

        // Keep normal Lua language features, but remove file/system/Java escape hatches.
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("luajava", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);

        globals.set("print", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                StringBuilder line = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) line.append("\t");
                    line.append(args.arg(i).tojstring());
                }
                appendConsoleThreadSafe("[PRINT] " + line);
                return LuaValue.NONE;
            }
        });

        globals.set("gg", createGgApi());
        return globals;
    }

    private LuaTable createGgApi() {
        final FakeMemoryManager memory = FakeMemoryManager.getInstance();
        LuaTable gg = new LuaTable();

        gg.set("TYPE_BYTE", LuaValue.valueOf(1));
        gg.set("TYPE_WORD", LuaValue.valueOf(2));
        gg.set("TYPE_DWORD", LuaValue.valueOf(4));
        gg.set("TYPE_XOR", LuaValue.valueOf(8));
        gg.set("TYPE_FLOAT", LuaValue.valueOf(16));
        gg.set("TYPE_QWORD", LuaValue.valueOf(32));
        gg.set("TYPE_DOUBLE", LuaValue.valueOf(64));
        gg.set("TYPE_AUTO", LuaValue.valueOf(127));

        gg.set("toast", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String message = args.arg(1).optjstring("");
                runOnUiThread(() -> Toast.makeText(ScriptExecutorActivity.this, message, Toast.LENGTH_SHORT).show());
                appendConsoleThreadSafe("[TOAST] " + message);
                return LuaValue.NONE;
            }
        });

        gg.set("alert", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String message = args.arg(1).optjstring("");
                runOnUiThread(() -> Toast.makeText(ScriptExecutorActivity.this, message, Toast.LENGTH_LONG).show());
                appendConsoleThreadSafe("[ALERT] " + message);
                return LuaValue.valueOf(1);
            }
        });

        gg.set("searchNumber", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int value = parseSyntheticInt(args.arg(1));
                int flags = args.arg(2).optint(127);
                memory.generateNewResults(value);
                int count = memory.getCurrentResults().size();
                appendConsoleThreadSafe("[SEARCH] " + value + " flags=" + flags + " -> " + count + " synthetic results");
                return LuaValue.NONE;
            }
        });

        gg.set("refineNumber", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int target = parseSyntheticInt(args.arg(1));
                List<FakeMemoryManager.MemoryResult> results = memory.getCurrentResults();
                Iterator<FakeMemoryManager.MemoryResult> iterator = results.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().valueDec != target) iterator.remove();
                }
                appendConsoleThreadSafe("[REFINE] value=" + target + " -> " + results.size() + " results");
                return LuaValue.NONE;
            }
        });

        gg.set("getResultsCount", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(memory.getCurrentResults().size());
            }
        });

        gg.set("getResults", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                List<FakeMemoryManager.MemoryResult> results = memory.getCurrentResults();
                int requested = args.arg(1).optint(results.size());
                int skip = Math.max(0, args.arg(2).optint(0));
                int end = Math.min(results.size(), skip + Math.max(0, requested));

                LuaTable output = new LuaTable();
                int luaIndex = 1;
                for (int i = skip; i < end; i++) {
                    FakeMemoryManager.MemoryResult item = results.get(i);
                    LuaTable row = new LuaTable();
                    row.set("address", LuaValue.valueOf(item.fakeAddress));
                    row.set("value", LuaValue.valueOf(item.valueDec));
                    row.set("flags", LuaValue.valueOf(4));
                    row.set("region", LuaValue.valueOf(item.region));
                    row.set("freeze", LuaValue.valueOf(item.isFrozen));
                    output.set(luaIndex++, row);
                }
                return output;
            }
        });

        gg.set("editAll", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int newValue = parseSyntheticInt(args.arg(1));
                List<FakeMemoryManager.MemoryResult> results = memory.getCurrentResults();
                for (FakeMemoryManager.MemoryResult item : results) item.setValue(newValue);
                appendConsoleThreadSafe("[EDIT ALL] " + results.size() + " results -> " + newValue);
                return LuaValue.NONE;
            }
        });

        gg.set("setValues", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaValue value = args.arg(1);
                if (!value.istable()) return LuaValue.FALSE;
                LuaTable table = value.checktable();
                List<FakeMemoryManager.MemoryResult> results = memory.getCurrentResults();
                int changed = 0;

                for (int i = 1; i <= table.length(); i++) {
                    LuaValue rowValue = table.get(i);
                    if (!rowValue.istable()) continue;
                    LuaTable row = rowValue.checktable();
                    String address = row.get("address").optjstring("");
                    LuaValue newValue = row.get("value");
                    if (address.isEmpty() || newValue.isnil()) continue;

                    for (FakeMemoryManager.MemoryResult item : results) {
                        if (item.fakeAddress.equalsIgnoreCase(address)) {
                            item.setValue(parseSyntheticInt(newValue));
                            changed++;
                            break;
                        }
                    }
                }

                appendConsoleThreadSafe("[SET VALUES] changed=" + changed);
                return LuaValue.TRUE;
            }
        });

        gg.set("addListItems", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaValue value = args.arg(1);
                if (!value.istable()) return LuaValue.FALSE;
                LuaTable table = value.checktable();
                List<FakeMemoryManager.MemoryResult> results = memory.getCurrentResults();
                int frozen = 0;

                for (int i = 1; i <= table.length(); i++) {
                    LuaValue rowValue = table.get(i);
                    if (!rowValue.istable()) continue;
                    LuaTable row = rowValue.checktable();
                    String address = row.get("address").optjstring("");
                    boolean freeze = row.get("freeze").optboolean(false);
                    if (!freeze || address.isEmpty()) continue;

                    for (FakeMemoryManager.MemoryResult item : results) {
                        if (item.fakeAddress.equalsIgnoreCase(address)) {
                            LuaValue desiredValue = row.get("value");
                            if (!desiredValue.isnil()) item.setValue(parseSyntheticInt(desiredValue));
                            item.isFrozen = true;
                            frozen++;
                            break;
                        }
                    }
                }

                appendConsoleThreadSafe("[LIST] frozen=" + frozen);
                return LuaValue.TRUE;
            }
        });

        gg.set("clearResults", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                memory.clearResults();
                appendConsoleThreadSafe("[CLEAR] Search results cleared");
                return LuaValue.NONE;
            }
        });

        gg.set("sleep", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                long requested = Math.max(0, args.arg(1).optlong(0));
                long delay = Math.min(requested, 5000);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return LuaValue.NONE;
            }
        });

        gg.set("setVisible", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                appendConsoleThreadSafe("[UI] gg.setVisible(" + args.arg(1).optboolean(false) + ") (simulated)");
                return LuaValue.NONE;
            }
        });

        gg.set("isVisible", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.FALSE;
            }
        });

        return gg;
    }

    private int parseSyntheticInt(LuaValue value) {
        if (value.isnumber()) return (int) Math.round(value.todouble());

        String raw = value.optjstring("0").trim();
        if (raw.endsWith("D") || raw.endsWith("d") || raw.endsWith("F") || raw.endsWith("f") || raw.endsWith("Q") || raw.endsWith("q")) {
            raw = raw.substring(0, raw.length() - 1).trim();
        }

        try {
            if (raw.startsWith("0x") || raw.startsWith("0X")) return (int) Long.parseLong(raw.substring(2), 16);
            return (int) Math.round(Double.parseDouble(raw));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unsupported numeric value: " + raw);
        }
    }

    private void appendConsoleThreadSafe(String line) {
        runOnUiThread(() -> appendConsoleLine(line));
    }

    private void appendConsoleLine(String line) {
        String currentText = consoleOutput.getText().toString();
        consoleOutput.setText(currentText + "\n" + line);
    }

    @Override
    protected void onDestroy() {
        scriptExecutor.shutdownNow();
        super.onDestroy();
    }
}
