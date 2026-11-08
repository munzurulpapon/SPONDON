package com.example.spondon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class SystemAlertReceiver extends BroadcastReceiver {

    public static final String ACTION_BATTERY_LOW = Intent.ACTION_BATTERY_LOW;
    public static final String ACTION_BATTERY_OKAY = Intent.ACTION_BATTERY_OKAY;
    public static final String ACTION_AIRPLANE_MODE = Intent.ACTION_AIRPLANE_MODE_CHANGED;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();

        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
            String message = isAirplaneModeOn
                    ? "✈️ SPONDON Alert: Airplane Mode is ON. Operating in offline civic mode."
                    : "🌐 SPONDON Alert: Airplane Mode is OFF. Municipal network sync restored.";
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            Toast.makeText(
                    context,
                    "⚠️ SPONDON Alert: Battery is LOW. Conserving GPS location & background civic sync.",
                    Toast.LENGTH_LONG
            ).show();
        } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
            Toast.makeText(
                    context,
                    "🔋 SPONDON Alert: Battery level restored to normal.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
