package com.ghosthacker.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Button launchButton = findViewById(R.id.launch_button);
        launchButton.setOnClickListener(this::launchFloatingIcon);
    }

    public void launchFloatingIcon(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "FATAL: DRAW_OVERLAY permission required for stealth mode.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
        } else {
            startFloatingService();
        }
    }

    private void startFloatingService() {
        Toast.makeText(this, "System Overlay initiated. Hooking to WindowManager...", Toast.LENGTH_SHORT).show();
        startService(new Intent(this, FloatingService.class));
        finish();
    }
}
