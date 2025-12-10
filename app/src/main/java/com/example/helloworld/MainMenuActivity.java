package com.ghosthacker.app;

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

        TextView status = findViewById(R.id.game_status);
        status.setText("ATTACHED: com.example.TargetGame (PID: 12345)");
    }

    public void onScriptSelectClicked(View view) {
        Toast.makeText(this, "実行環境を準備中...", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, ScriptExecutorActivity.class));
        finish(); 
    }

    public void onMemorySearchClicked(View view) {
        Toast.makeText(this, "コンソールを起動します。", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MemorySearchActivity.class));
        finish();
    }

    public void onCloseClicked(View view) {
        Toast.makeText(this, "変更しました", Toast.LENGTH_SHORT).show();
        finish();
    }
}
