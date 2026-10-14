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

public class InjectionService extends Service {
    private WindowManager windowManager;
    private ImageView floatingIcon;
    private WindowManager.LayoutParams params;
    private String targetProcess;
    private String targetPid;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetProcess = intent.getStringExtra("TARGET_PROCESS");
            targetPid = intent.getStringExtra("TARGET_PID");
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingIcon = new ImageView(this);
        floatingIcon.setImageResource(R.mipmap.ic_launcher);
        floatingIcon.setContentDescription("Open Titan Guard simulator");

        int iconSize = (int) (getResources().getDisplayMetrics().density * 48);
        params = new WindowManager.LayoutParams(
                iconSize,
                iconSize,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        windowManager.addView(floatingIcon, params);

        floatingIcon.setOnTouchListener(new View.OnTouchListener() {
            private static final int CLICK_DURATION_MS = 250;
            private static final int LONG_PRESS_DURATION_MS = 700;
            private static final float MOVE_THRESHOLD_PX = 12f;

            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchStartTime;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > MOVE_THRESHOLD_PX || Math.abs(dy) > MOVE_THRESHOLD_PX) {
                            moved = true;
                            params.x = initialX + (int) dx;
                            params.y = initialY + (int) dy;
                            windowManager.updateViewLayout(floatingIcon, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - touchStartTime;
                        if (!moved && duration >= LONG_PRESS_DURATION_MS) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Speed panel simulation · no game timing is changed",
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        if (!moved && duration < CLICK_DURATION_MS) {
                            Intent panel = new Intent(getApplicationContext(), MainMenuActivity.class);
                            panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            panel.putExtra("TARGET_PROCESS", targetProcess == null ? "Synthetic Demo Game" : targetProcess);
                            panel.putExtra("TARGET_PID", targetPid == null ? "SIM" : targetPid);
                            startActivity(panel);
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        if (floatingIcon != null && windowManager != null) {
            try {
                windowManager.removeView(floatingIcon);
            } catch (IllegalArgumentException ignored) {
            }
        }
        super.onDestroy();
    }
}
