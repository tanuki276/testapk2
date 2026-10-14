package com.tci.injector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainMenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        TextView status = findViewById(R.id.process_status);
        String targetProcess = getIntent().getStringExtra("TARGET_PROCESS");
        if (targetProcess == null || targetProcess.trim().isEmpty()) targetProcess = "Synthetic Demo Game";
        status.setText("TARGET  ·  " + targetProcess + "  ·  SAFE SIMULATION");
    }

    public void onMemorySearchClicked(View view) {
        startActivity(new Intent(this, MemorySearchActivity.class));
    }

    public void onSavedListClicked(View view) {
        Intent intent = new Intent(this, MemorySearchActivity.class);
        intent.putExtra("SHOW_SAVED", true);
        startActivity(intent);
    }

    public void onScriptSelectClicked(View view) {
        startActivity(new Intent(this, ScriptExecutorActivity.class));
    }

    public void onSettingsClicked(View view) {
        Toast.makeText(this, "Simulator settings: floating icon + synthetic memory only", Toast.LENGTH_LONG).show();
    }

    public void onCloseClicked(View view) {
        finish();
    }

    public void onStopOverlayClicked(View view) {
        stopService(new Intent(this, InjectionService.class));
        Toast.makeText(this, "Floating tool stopped", Toast.LENGTH_SHORT).show();
        finish();
    }
}
