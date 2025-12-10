package com.tci.injector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tci.injector.R;

public class MainMenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        TextView status = findViewById(R.id.process_status);
        
        String targetProcess = getIntent().getStringExtra("TARGET_PROCESS");
        String targetPid = getIntent().getStringExtra("TARGET_PID");
        
        if (targetProcess != null && targetPid != null) {
            status.setText("ATTACHED: " + targetProcess + " (PID: " + targetPid + ")");
        } else {
            status.setText("STATUS: Active on Primary Thread (PID: " + android.os.Process.myPid() + ")");
        }
    }

    public void onScriptSelectClicked(View view) {
        Toast.makeText(this, "TCI: Loading Script Engine Module.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, ScriptExecutorActivity.class));
        finish(); 
    }

    public void onMemorySearchClicked(View view) {
        Toast.makeText(this, "TCI: Initiating Memory Region Access.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MemorySearchActivity.class));
        finish();
    }

    public void onCloseClicked(View view) {
        Toast.makeText(this, "TCI: Main Module Shutting Down.", Toast.LENGTH_SHORT).show();
        
        Intent serviceIntent = new Intent(this, InjectionService.class); 
        stopService(serviceIntent); 
        
        finish();
    }
}
