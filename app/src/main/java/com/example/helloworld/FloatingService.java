package com.tci.injector;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.tci.injector.R;

public class InjectionService extends Service { // クラス名を変更
    private WindowManager windowManager;
    private ImageView floatingIcon;
    private WindowManager.LayoutParams params;
    
    // ターゲットプロセス情報を保持
    private String targetProcess;
    private String targetPid;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetProcess = intent.getStringExtra("TARGET_PROCESS");
            targetPid = intent.getStringExtra("TARGET_PID");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        floatingIcon = new ImageView(this);
        // アイコンをアプリのランチャーアイコンに変更
        floatingIcon.setImageResource(R.mipmap.ic_launcher); 

        // パラメータの設定 (サイズを小さくする)
        int iconSize = (int) (getResources().getDisplayMetrics().density * 48); // 48dp
        
        params = new WindowManager.LayoutParams(
            iconSize, // WRAP_CONTENT から固定サイズに変更
            iconSize, // WRAP_CONTENT から固定サイズに変更
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        windowManager.addView(floatingIcon, params);

        floatingIcon.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long touchStartTime;
            private boolean isClick;
            
            // GG風: 長押し判定用の変数
            private static final int CLICK_DURATION_MS = 200;
            private static final int LONG_PRESS_DURATION_MS = 1000; 

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        isClick = true;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // 移動距離が閾値を超えたらクリックではないと判定し、アイコンを移動
                        if (Math.abs(event.getRawX() - initialTouchX) > 10 || Math.abs(event.getRawY() - initialTouchY) > 10) {
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingIcon, params);
                            isClick = false; 
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - touchStartTime;

                        if (isClick && (duration < CLICK_DURATION_MS)) {
                            // 短いタップ: GG風メニュー（MainMenuActivity）を起動
                            Toast.makeText(getApplicationContext(), "TCI: Accessing Main Module.", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(getApplicationContext(), MainMenuActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            // ターゲット情報をメニューに渡す
                            intent.putExtra("TARGET_PROCESS", targetProcess);
                            intent.putExtra("TARGET_PID", targetPid);
                            startActivity(intent);
                        } else if (!isClick && duration > LONG_PRESS_DURATION_MS) {
                            // 長押し（ゴミ箱機能シミュレーション）: サービスを停止
                            Toast.makeText(getApplicationContext(), "TCI: Injection Service Terminated.", Toast.LENGTH_LONG).show();
                            stopSelf();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingIcon != null) windowManager.removeView(floatingIcon);
    }
}
