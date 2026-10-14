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
import android.widget.Toast;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Floating Lua UI layer. Standard one-shot dialogs are kept for compatibility,
 * while toggleMenu provides a persistent menu whose rows change color in place.
 */
public final class LuaOverlayUi {
    private static final int TOGGLE_CLOSE_EVENT = -1;

    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private View activeView;

    private LinearLayout toggleMenuCard;
    private TextView toggleMenuTitle;
    private TextView toggleMenuStatus;
    private Button[] toggleButtons;
    private String[] toggleLabels;
    private boolean[] toggleStates;
    private final BlockingQueue<Integer> toggleEvents = new LinkedBlockingQueue<>();

    public LuaOverlayUi(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * Persistent colored toggle menu.
     * The window remains visible after a row is tapped. The selected zero-based
     * index is returned to Lua so the script can perform its action, then call
     * this method again with updated states. Closing the X returns null.
     */
    public Integer showToggleMenu(String title, String[] items, boolean[] states) {
        CountDownLatch ready = new CountDownLatch(1);

        mainHandler.post(() -> {
            boolean needsCreate = toggleMenuCard == null
                    || toggleButtons == null
                    || toggleLabels == null
                    || !Arrays.equals(toggleLabels, items);

            if (needsCreate) {
                createToggleMenu(title, items, states);
            } else {
                if (toggleMenuTitle != null) {
                    toggleMenuTitle.setText(title == null || title.isEmpty() ? "脚本菜单" : title);
                }
                syncToggleStates(states);
            }
            ready.countDown();
        });

        await(ready);

        try {
            int event = toggleEvents.take();
            return event == TOGGLE_CLOSE_EVENT ? null : event;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void createToggleMenu(String title, String[] items, boolean[] states) {
        removeActiveInternal(false);
        toggleEvents.clear();
        toggleLabels = items.clone();
        toggleStates = new boolean[items.length];
        if (states != null) {
            System.arraycopy(states, 0, toggleStates, 0, Math.min(states.length, toggleStates.length));
        }

        LinearLayout card = createCard();
        toggleMenuCard = card;

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title == null || title.isEmpty() ? "脚本菜单" : title);
        titleView.setTextColor(0xFF42D5E8);
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        toggleMenuTitle = titleView;
        header.addView(titleView, new LinearLayout.LayoutParams(0, dp(44), 1f));

        Button close = createActionButton("×");
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(46), dp(40));
        close.setOnClickListener(v -> {
            removeActiveInternal(false);
            toggleEvents.offer(TOGGLE_CLOSE_EVENT);
        });
        header.addView(close, closeParams);
        card.addView(header, matchWrap(0));

        ScrollView scroll = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        toggleButtons = new Button[items.length];

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            Button item = createMenuButton(items[i]);
            applyToggleStyle(item, toggleStates[i]);
            item.setOnClickListener(v -> {
                boolean next = !toggleStates[index];
                toggleStates[index] = next;
                applyToggleStyle(toggleButtons[index], next);
                if (toggleMenuStatus != null) {
                    toggleMenuStatus.setText(items[index] + (next ? "  ·  已开启" : "  ·  已关闭"));
                    toggleMenuStatus.setTextColor(next ? 0xFF62E69A : 0xFFA7B2C2);
                }
                toggleEvents.offer(index);
            });
            toggleButtons[i] = item;
            list.addView(item, matchWrap(7));
        }

        scroll.addView(list);
        card.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(360)));

        TextView status = new TextView(context);
        status.setText("点击项目直接切换 · × 隐藏菜单");
        status.setTextColor(0xFFA7B2C2);
        status.setTextSize(12);
        status.setPadding(dp(4), dp(10), dp(4), dp(2));
        toggleMenuStatus = status;
        card.addView(status, matchWrap(0));

        activeView = card;
        windowManager.addView(card, createParams());
    }

    private void syncToggleStates(boolean[] states) {
        if (toggleButtons == null || toggleStates == null) return;
        for (int i = 0; i < toggleButtons.length; i++) {
            boolean enabled = states != null && i < states.length && states[i];
            toggleStates[i] = enabled;
            applyToggleStyle(toggleButtons[i], enabled);
        }
    }

    private void applyToggleStyle(Button button, boolean enabled) {
        if (button == null) return;
        if (enabled) {
            button.setBackground(roundRect(0xFF176B46, 10, 0xFF43D88A));
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackground(roundRect(0xFF1B2430, 10, 0xFF2C394A));
            button.setTextColor(0xFFE9EEF5);
        }
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
                    removeActiveInternal(false);
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
                removeActiveInternal(false);
                latch.countDown();
            });
            ok.setOnClickListener(v -> {
                boolean[] values = new boolean[checks.length];
                for (int i = 0; i < checks.length; i++) values[i] = checks[i].isChecked();
                result.set(values);
                removeActiveInternal(false);
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
                removeActiveInternal(false);
                latch.countDown();
            });
            ok.setOnClickListener(v -> {
                String[] values = new String[fields.length];
                for (int i = 0; i < fields.length; i++) values[i] = fields[i].getText().toString();
                result.set(values);
                removeActiveInternal(false);
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
                    removeActiveInternal(false);
                    latch.countDown();
                });
                actions.addView(no, weighted());
            }

            Button yes = createActionButton(positive == null || positive.isEmpty() ? "OK" : positive);
            yes.setOnClickListener(v -> {
                result.set(1);
                removeActiveInternal(false);
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
        mainHandler.post(() -> removeActiveInternal(true));
    }

    private void show(View view) {
        removeActiveInternal(true);
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
            removeActiveInternal(false);
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

    private void removeActiveInternal(boolean signalToggleClose) {
        boolean wasToggle = activeView != null && activeView == toggleMenuCard;
        if (activeView != null) {
            try {
                windowManager.removeView(activeView);
            } catch (IllegalArgumentException ignored) {
            }
        }
        activeView = null;

        if (wasToggle) {
            toggleMenuCard = null;
            toggleMenuTitle = null;
            toggleMenuStatus = null;
            toggleButtons = null;
            toggleLabels = null;
            toggleStates = null;
            if (signalToggleClose) toggleEvents.offer(TOGGLE_CLOSE_EVENT);
        }
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
