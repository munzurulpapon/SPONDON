package com.example.spondon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class CivicMonitoringService extends Service {

    public static final String CHANNEL_ID = "spondon_civic_foreground";
    public static final int NOTIFICATION_ID = 2026;
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";

    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            try { stopForeground(true); } catch (Exception ignored) {}
            stopSelf();
            isRunning = false;
            return START_NOT_STICKY;
        }

        // Android 12+ throws if a background restart tries to promote the service.
        // Never let that close the app: stop quietly instead.
        try {
            Notification notification = buildForegroundNotification();
            startForeground(NOTIFICATION_ID, notification);
            isRunning = true;
        } catch (Exception e) {
            android.util.Log.w("CivicMonitoringService", "Foreground start refused: " + e.getMessage());
            isRunning = false;
            stopSelf();
            return START_NOT_STICKY;
        }

        // NOT_STICKY: the system must not silently relaunch us from the background.
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SPONDON Civic Emergency Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Real-time city alerts, emergency hotline readiness, and municipal services monitor");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildForegroundNotification() {
        Intent openIntent = new Intent(this, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Intent sosIntent = new Intent(this, EmergencyRequestActivity.class);
        PendingIntent sosPendingIntent = PendingIntent.getActivity(
                this, 1, sosIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Intent stopIntent = new Intent(this, CivicMonitoringService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 2, stopIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SPONDON Smart City Monitor Active")
                .setContentText("Emergency SOS hotline & municipal alert guard active in Khulna")
                .setSmallIcon(R.drawable.ic_nav_notifications)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_dialog_alert, "Open SOS", sosPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, CivicMonitoringService.class);
        intent.setAction(ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            android.util.Log.w("CivicMonitoringService", "Unable to start foreground service: " + e.getMessage());
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, CivicMonitoringService.class);
        intent.setAction(ACTION_STOP);
        try {
            context.startService(intent);
        } catch (Exception ignored) {}
    }
}
