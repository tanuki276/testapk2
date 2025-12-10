package com.tci.injector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.tci.injector.R;

public class MainActivity extends Activity {

    private Spinner targetSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetSpinner = findViewById(R.id.target_spinner);
        Button attachButton = findViewById(R.id.attach_button);
        
        attachButton.setOnClickListener(this::onAttachClicked);
    }

    public void onAttachClicked(View view) {
        String selectedProcess = targetSpinner.getSelectedItem().toString();
        
        if (selectedProcess.startsWith("None Selected")) {
            Toast.makeText(this, "ERROR: Target process selection required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "FATAL: SYSTEM_OVERLAY privilege required for injection service.", Toast.LENGTH_LONG).show();
            
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
        } else {
            // 権限がある場合、または旧バージョンでサービスを起動
            startInjectionService(selectedProcess);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // 権限取得成功後、サービスを再起動
                startInjectionService(targetSpinner.getSelectedItem().toString());
            } else {
                Toast.makeText(this, "CRITICAL: Privilege denied. Cannot proceed with injection.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startInjectionService(String targetProcessInfo) {
        String processName = targetProcessInfo.substring(0, targetProcessInfo.indexOf(" ("));
        String pid = targetProcessInfo.substring(targetProcessInfo.indexOf("PID: ") + 5, targetProcessInfo.length() - 1);
        
        Toast.makeText(this, 
            "TCI: Attaching to " + processName + ". PID: " + pid, 
            Toast.LENGTH_SHORT).show();
            
        Intent serviceIntent = new Intent(this, InjectionService.class);
        serviceIntent.putExtra("TARGET_PROCESS", processName);
        serviceIntent.putExtra("TARGET_PID", pid);
        
        startService(serviceIntent);
        finish();
    }
}
