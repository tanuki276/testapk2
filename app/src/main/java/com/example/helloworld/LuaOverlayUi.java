package com.tci.injector;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Small GameGuardian-style popup layer for Lua scripts.
 * It uses Android overlay windows, so the host app can stay in the background.
 */
public final class LuaOverlayUi {
    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private View activeView;

    public LuaOverlayUi(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    public Integer showChoice(String title, String[] items, int selected) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> result = new AtomicReference<>(null);

        mainHandler.post(() -> {
            LinearLayout card = createCard();
            addTitle(card, title);

            ScrollView scroll = new ScrollView(context);
            LinearLayout list = new LinearLayout(context);
            list.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(list);

            for (int i = 0; i < items.length; i++) {
                final int index = i;
                Button item = createMenuButton(items[i]);
                if (i == selected) item.setText("✓  " + items[i]);
                item.setOnClickListener(v -> {
                    result.set(index);
                    removeActive();
                    latch.countDown();
                });
                list.addView(item, matchWrap(8));
            }

            card.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(360)));
            addCancel(card, latch);
            show(card);
        });

        await(latch);
        return result.get();
    }

    public boolean[] showMultiChoice(String title, String[] items, boolean[] defaults) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<boolean[]> result = new AtomicReference<>(null);

        mainHandler.post(() -> {
            LinearLayout card = createCard();
            addTitle(card, title);

            ScrollView scroll = new ScrollView(context);
            LinearLayout list = new LinearLayout(context);
            list.setOrientation(LinearLayout.VERTICAL);
            CheckBox[] checks = new CheckBox[items.length];

            for (int i = 0; i < items.length; i++) {
                CheckBox box = new CheckBox(context);
                box.setText(items[i]);
                box.setTextColor(Color.WHITE);
                box.setTextSize(16);
                box.setPadding(dp(8), dp(8), dp(8), dp(8));
                box.setChecked(defaults != null && i < defaults.length && defaults[i]);
                checks[i] = box;
                list.addView(box, matchWrap(4));
            }

            scroll.addView(list);
            card.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(320)));

            LinearLayout actions = horizontalActions();
            Button cancel = createActionButton("取消");
            Button ok = createActionButton("确定");
            cancel.setOnClickListener(v -> {
                removeActive();
                latch.countDown();
            });
            ok.setOnClickListener(v -> {
                boolean[] values = new boolean[checks.length];
                for (int i = 0; i < checks.length; i++) values[i] = checks[i].isChecked();
                result.set(values);
                removeActive();
                latch.countDown();
            });
            actions.addView(cancel, weighted());
            actions.addView(ok, weighted());
            card.addView(actions, matchWrap(8));
            show(card);
        });

        await(latch);
        return result.get();
    }

    public String[] showPrompt(String title, String[] labels, String[] defaults) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String[]> result = new AtomicReference<>(null);

        mainHandler.post(() -> {
            LinearLayout card = createCard();
            addTitle(card, title);
            EditText[] fields = new EditText[labels.length];

            for (int i = 0; i < labels.length; i++) {
                TextView label = new TextView(context);
                label.setText(labels[i]);
                label.setTextColor(0xFFB7C4D3);
                label.setTextSize(13);
                label.setPadding(0, dp(8), 0, dp(4));
                card.addView(label, matchWrap(0));

                EditText input = new EditText(context);
                input.setTextColor(Color.WHITE);
                input.setHintTextColor(0xFF7D8A99);
                input.setText(defaults != null && i < defaults.length && defaults[i] != null ? defaults[i] : "");
                input.setSingleLine(true);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setPadding(dp(10), dp(8), dp(10), dp(8));
                input.setBackground(roundRect(0xFF1B2430, 10, 0xFF334155));
                fields[i] = input;
                card.addView(input, matchWrap(4));
            }

            LinearLayout actions = horizontalActions();
            Button cancel = createActionButton("取消");
            Button ok = createActionButton("确定");
            cancel.setOnClickListener(v -> {
                removeActive();
                latch.countDown();
            });
            ok.setOnClickListener(v -> {
                String[] values = new String[fields.length];
                for (int i = 0; i < fields.length; i++) values[i] = fields[i].getText().toString();
                result.set(values);
                removeActive();
                latch.countDown();
            });
            actions.addView(cancel, weighted());
            actions.addView(ok, weighted());
            card.addView(actions, matchWrap(10));
            show(card);
        });

        await(latch);
        return result.get();
    }

    public int showAlert(String message, String positive, String negative) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> result = new AtomicReference<>(0);

        mainHandler.post(() -> {
            LinearLayout card = createCard();
            TextView body = new TextView(context);
            body.setText(message);
            body.setTextColor(Color.WHITE);
            body.setTextSize(16);
            body.setPadding(0, dp(4), 0, dp(10));
            card.addView(body, matchWrap(0));

            LinearLayout actions = horizontalActions();
            if (negative != null && !negative.isEmpty()) {
                Button no = createActionButton(negative);
                no.setOnClickListener(v -> {
                    result.set(2);
                    removeActive();
                    latch.countDown();
                });
                actions.addView(no, weighted());
            }

            Button yes = createActionButton(positive == null || positive.isEmpty() ? "OK" : positive);
            yes.setOnClickListener(v -> {
                result.set(1);
                removeActive();
                latch.countDown();
            });
            actions.addView(yes, weighted());
            card.addView(actions, matchWrap(4));
            show(card);
        });

        await(latch);
        return result.get();
    }

    public void dismissActive() {
        mainHandler.post(this::removeActive);
    }

    private void show(View view) {
        removeActive();
        activeView = view;
        windowManager.addView(view, createParams());
    }

    private WindowManager.LayoutParams createParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(340),
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        return params;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundRect(0xF2121820, 14, 0xFF344254));
        return card;
    }

    private void addTitle(LinearLayout parent, String title) {
        TextView text = new TextView(context);
        text.setText(title == null || title.isEmpty() ? "脚本菜单" : title);
        text.setTextColor(0xFF42D5E8);
        text.setTextSize(18);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setPadding(0, 0, 0, dp(8));
        parent.addView(text, matchWrap(0));
    }

    private Button createMenuButton(String text) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setPadding(dp(14), 0, dp(12), 0);
        button.setBackground(roundRect(0xFF1B2430, 10, 0xFF2C394A));
        return button;
    }

    private Button createActionButton(String text) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(roundRect(0xFF243142, 9, 0xFF3A4A60));
        return button;
    }

    private void addCancel(LinearLayout card, CountDownLatch latch) {
        Button close = createActionButton("关闭");
        close.setOnClickListener(v -> {
            removeActive();
            latch.countDown();
        });
        card.addView(close, matchWrap(10));
    }

    private LinearLayout horizontalActions() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1f);
        p.setMargins(dp(4), 0, dp(4), 0);
        return p;
    }

    private LinearLayout.LayoutParams matchWrap(int topMarginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(topMarginDp);
        return p;
    }

    private GradientDrawable roundRect(int fillColor, int radiusDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private void removeActive() {
        if (activeView == null) return;
        try {
            windowManager.removeView(activeView);
        } catch (IllegalArgumentException ignored) {
        }
        activeView = null;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
