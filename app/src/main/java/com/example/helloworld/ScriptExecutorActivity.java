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
    private LuaOverlayUi overlayUi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_executor);

        overlayUi = new LuaOverlayUi(this);
        consoleOutput = findViewById(R.id.script_console_output);
        scriptPathDisplay = findViewById(R.id.script_path_display);
        scriptEditor = findViewById(R.id.script_editor);
        Button selectScriptButton = findViewById(R.id.select_script_button);
        executeScriptButton = findViewById(R.id.execute_script_button);

        appendConsoleLine("[TITAN LUA]");
        appendConsoleLine("> Lua engine ready.");
        appendConsoleLine("> GUI: gg.choice / gg.multiChoice / gg.prompt / gg.alert");
        appendConsoleLine("> Floating icon controls script UI visibility.");

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
            appendConsoleLine("> [LOAD] " + name);
            Toast.makeText(this, "Lua loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendConsoleLine("> [ERROR] " + e.getMessage());
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
            Toast.makeText(this, "Load a .lua file first", Toast.LENGTH_SHORT).show();
            return;
        }

        executeScriptButton.setEnabled(false);
        appendConsoleLine("\n> [EXEC] Running Lua...");

        // Match the usual floating-tool workflow: leave the host screen and let
        // the script show only the popup it needs over the foreground app.
        LuaRuntimeState.setActive(true);
        LuaRuntimeState.hideUi();
        moveTaskToBack(true);

        scriptExecutor.execute(() -> {
            try {
                Globals globals = createSandboxGlobals();
                globals.load(source, "titan-script.lua").call();
                appendConsoleThreadSafe("[DONE] Lua finished.");
            } catch (Throwable error) {
                String message = error.getMessage();
                appendConsoleThreadSafe("[LUA ERROR] " + (message == null ? error.getClass().getSimpleName() : message));
            } finally {
                overlayUi.dismissActive();
                LuaRuntimeState.setActive(false);
                runOnUiThread(() -> executeScriptButton.setEnabled(true));
            }
        });
    }

    private Globals createSandboxGlobals() {
        Globals globals = JsePlatform.standardGlobals();
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
                return LuaValue.NONE;
            }
        });

        gg.set("alert", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String message = args.arg(1).optjstring("");
                String positive = args.arg(2).optjstring("OK");
                String negative = args.arg(3).optjstring("");
                return LuaValue.valueOf(overlayUi.showAlert(message, positive, negative));
            }
        });

        gg.set("choice", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaValue itemsValue = args.arg(1);
                if (!itemsValue.istable()) return LuaValue.NIL;
                String[] items = luaArrayToStrings(itemsValue.checktable());
                int selected = args.arg(2).isnil() ? -1 : Math.max(0, args.arg(2).optint(1) - 1);
                String title = args.arg(3).optjstring("菜单");
                Integer result = overlayUi.showChoice(title, items, selected);
                return result == null ? LuaValue.NIL : LuaValue.valueOf(result + 1);
            }
        });

        gg.set("multiChoice", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaValue itemsValue = args.arg(1);
                if (!itemsValue.istable()) return LuaValue.NIL;
                String[] items = luaArrayToStrings(itemsValue.checktable());
                boolean[] checked = new boolean[items.length];
                LuaValue defaults = args.arg(2);
                if (defaults.istable()) {
                    LuaTable table = defaults.checktable();
                    for (int i = 0; i < checked.length; i++) checked[i] = table.get(i + 1).optboolean(false);
                }
                String title = args.arg(3).optjstring("菜单");
                boolean[] result = overlayUi.showMultiChoice(title, items, checked);
                if (result == null) return LuaValue.NIL;
                LuaTable out = new LuaTable();
                for (int i = 0; i < result.length; i++) if (result[i]) out.set(i + 1, LuaValue.TRUE);
                return out;
            }
        });

        gg.set("prompt", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaValue labelsValue = args.arg(1);
                if (!labelsValue.istable()) return LuaValue.NIL;
                String[] labels = luaArrayToStrings(labelsValue.checktable());
                String[] defaults = new String[labels.length];
                LuaValue defaultsValue = args.arg(2);
                if (defaultsValue.istable()) {
                    LuaTable table = defaultsValue.checktable();
                    for (int i = 0; i < labels.length; i++) defaults[i] = table.get(i + 1).optjstring("");
                }
                String[] values = overlayUi.showPrompt("输入", labels, defaults);
                if (values == null) return LuaValue.NIL;
                LuaTable out = new LuaTable();
                for (int i = 0; i < values.length; i++) out.set(i + 1, LuaValue.valueOf(values[i]));
                return out;
            }
        });

        gg.set("searchNumber", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int value = parseSyntheticInt(args.arg(1));
                memory.generateNewResults(value);
                appendConsoleThreadSafe("[SEARCH] " + value + " -> " + memory.getCurrentResults().size());
                return LuaValue.NONE;
            }
        });

        gg.set("refineNumber", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int target = parseSyntheticInt(args.arg(1));
                List<FakeMemoryManager.MemoryResult> results = memory.getCurrentResults();
                Iterator<FakeMemoryManager.MemoryResult> iterator = results.iterator();
                while (iterator.hasNext()) if (iterator.next().valueDec != target) iterator.remove();
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
                int index = 1;
                for (int i = skip; i < end; i++) {
                    FakeMemoryManager.MemoryResult item = results.get(i);
                    LuaTable row = new LuaTable();
                    row.set("address", LuaValue.valueOf(item.fakeAddress));
                    row.set("value", LuaValue.valueOf(item.valueDec));
                    row.set("flags", LuaValue.valueOf(4));
                    row.set("region", LuaValue.valueOf(item.region));
                    row.set("freeze", LuaValue.valueOf(item.isFrozen));
                    output.set(index++, row);
                }
                return output;
            }
        });

        gg.set("editAll", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int value = parseSyntheticInt(args.arg(1));
                for (FakeMemoryManager.MemoryResult item : memory.getCurrentResults()) item.setValue(value);
                return LuaValue.NONE;
            }
        });

        gg.set("setValues", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaValue value = args.arg(1);
                if (!value.istable()) return LuaValue.FALSE;
                LuaTable table = value.checktable();
                for (int i = 1; i <= table.length(); i++) {
                    LuaValue rowValue = table.get(i);
                    if (!rowValue.istable()) continue;
                    LuaTable row = rowValue.checktable();
                    FakeMemoryManager.MemoryResult item = memory.findByAddress(row.get("address").optjstring(""));
                    if (item != null && !row.get("value").isnil()) item.setValue(parseSyntheticInt(row.get("value")));
                }
                return LuaValue.TRUE;
            }
        });

        gg.set("addListItems", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaValue value = args.arg(1);
                if (!value.istable()) return LuaValue.FALSE;
                LuaTable table = value.checktable();
                for (int i = 1; i <= table.length(); i++) {
                    LuaValue rowValue = table.get(i);
                    if (!rowValue.istable()) continue;
                    LuaTable row = rowValue.checktable();
                    FakeMemoryManager.MemoryResult item = memory.findByAddress(row.get("address").optjstring(""));
                    if (item != null) {
                        if (!row.get("value").isnil()) item.setValue(parseSyntheticInt(row.get("value")));
                        item.isFrozen = row.get("freeze").optboolean(item.isFrozen);
                        memory.saveResult(item);
                    }
                }
                return LuaValue.TRUE;
            }
        });

        gg.set("clearResults", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                memory.clearResults();
                return LuaValue.NONE;
            }
        });

        gg.set("sleep", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                try {
                    Thread.sleep(Math.min(Math.max(0, args.arg(1).optlong(0)), 5000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return LuaValue.NONE;
            }
        });

        // Follow GameGuardian's visibility pattern: scripts can hide the UI,
        // wait for the floating icon to be tapped, then open their menu again.
        gg.set("setVisible", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.arg(1).optboolean(true)) LuaRuntimeState.requestUi();
                else LuaRuntimeState.hideUi();
                return LuaValue.NONE;
            }
        });

        gg.set("isVisible", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(LuaRuntimeState.isUiRequested());
            }
        });

        return gg;
    }

    private String[] luaArrayToStrings(LuaTable table) {
        int len = table.length();
        String[] values = new String[len];
        for (int i = 0; i < len; i++) values[i] = table.get(i + 1).tojstring();
        return values;
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
        consoleOutput.setText(consoleOutput.getText().toString() + "\n" + line);
    }

    @Override
    protected void onDestroy() {
        overlayUi.dismissActive();
        LuaRuntimeState.setActive(false);
        scriptExecutor.shutdownNow();
        super.onDestroy();
    }
}
