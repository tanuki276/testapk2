package com.tci.injector;

import java.util.concurrent.atomic.AtomicBoolean;

/** Shared visibility state between the floating icon and a running Lua script. */
public final class LuaRuntimeState {
    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);
    private static final AtomicBoolean UI_REQUESTED = new AtomicBoolean(false);

    private LuaRuntimeState() {}

    public static void setActive(boolean active) {
        ACTIVE.set(active);
        if (!active) UI_REQUESTED.set(false);
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static void requestUi() {
        UI_REQUESTED.set(true);
    }

    public static void hideUi() {
        UI_REQUESTED.set(false);
    }

    public static boolean isUiRequested() {
        return UI_REQUESTED.get();
    }
}
