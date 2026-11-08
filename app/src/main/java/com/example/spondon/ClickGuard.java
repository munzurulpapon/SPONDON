package com.example.spondon;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

/**
 * Small helper that makes navigation taps reliable:
 *  - one tap opens the screen (whole card is the tap target, not just the inner text)
 *  - rapid repeat taps are ignored (no duplicate screens / double-launch crashes)
 *  - a missing or failing Activity shows a Toast instead of crashing the app
 */
public final class ClickGuard {

    private static final String TAG = "ClickGuard";
    private static final long DEBOUNCE_MS = 700;
    private static long lastClickAt = 0;

    private ClickGuard() {}

    /** Returns true if this tap should be handled, false if it is a repeat tap. */
    public static boolean allow() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastClickAt < DEBOUNCE_MS) return false;
        lastClickAt = now;
        return true;
    }

    /** Safe, debounced startActivity. */
    public static void open(Activity from, Class<?> target) {
        open(from, new Intent(from, target));
    }

    public static void open(Activity from, Intent intent) {
        if (from == null || from.isFinishing()) return;
        if (!allow()) return;
        launch(from, intent);
    }

    /** Starts the screen without debouncing (caller already debounced). */
    private static void launch(Activity from, Intent intent) {
        try {
            from.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Screen not registered in manifest", e);
            Toast.makeText(from, "This screen is not available.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unable to open screen", e);
            Toast.makeText(from, "Unable to open this screen. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Makes BOTH the card and the button inside it open the screen on a single tap.
     * Either view may be null.
     */
    public static void bind(Activity activity, View card, View button, Class<?> target) {
        bind(activity, card, button, () -> launch(activity, new Intent(activity, target)));
    }

    public static void bind(Activity activity, View card, View button, Runnable action) {
        View.OnClickListener l = v -> {
            if (activity.isFinishing() || !allow()) return;
            try {
                action.run();
            } catch (Exception e) {
                Log.e(TAG, "Click action failed", e);
                Toast.makeText(activity, "Unable to open this screen. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        if (button != null) {
            button.setOnClickListener(l);
        }
        if (card != null && card != button) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(l);
            if (Build.VERSION.SDK_INT >= 23) {
                TypedValue tv = new TypedValue();
                if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
                    card.setForeground(activity.getDrawable(tv.resourceId));
                }
            }
        }
    }
}
