package com.tci.injector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

/**
 * Launcher for the safe GameGuardian-style simulation.
 * No external process is attached or modified.
 */
public class MainActivity extends Activity {
    private static final int OVERLAY_PERMISSION_REQUEST = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onStartOverlayClicked(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow display over other apps to show the floating tool.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            return;
        }
        startSimulationOverlay();
    }

    public void onOpenPanelClicked(View view) {
        startActivity(new Intent(this, MainMenuActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                startSimulationOverlay();
            } else {
                Toast.makeText(this, "Overlay permission was not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startSimulationOverlay() {
        Intent serviceIntent = new Intent(this, InjectionService.class);
        serviceIntent.putExtra("TARGET_PROCESS", "Synthetic Demo Game");
        serviceIntent.putExtra("TARGET_PID", "SIM");
        startService(serviceIntent);
        Toast.makeText(this, "Floating simulator started", Toast.LENGTH_SHORT).show();
        finish();
    }
}
