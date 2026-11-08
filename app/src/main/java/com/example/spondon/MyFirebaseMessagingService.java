
package com.example.spondon;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "spondon_updates";
    private static final String TAG = "SPONDON_FCM";

    // Register current device token
    public static void registerToken(Context context) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "User not logged in. Token registration skipped.");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> saveToken(token))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Unable to get FCM token", e));
    }

    // Save token in Firestore
    private static void saveToken(String token) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "No authenticated user. Token not saved.");
            return;
        }

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        Map<String, Object> data = new HashMap<>();

        data.put("fcmToken", token);

        // IMPORTANT: Must match Firestore Rules
        data.put("fcmTokenUpdatedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "FCM token saved successfully for: " + uid))
                .addOnFailureListener(e ->
                        Log.e(TAG, "FCM token Firestore save failed", e));
    }

    // Called when Firebase refreshes the device token
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Log.d(TAG, "New FCM token received.");

        saveToken(token);
    }

    // Receive notification
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "SPONDON Update";
        String message = "You have a new project update.";

        if (remoteMessage.getNotification() != null) {

            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }

            if (remoteMessage.getNotification().getBody() != null) {
                message = remoteMessage.getNotification().getBody();
            }
        }

        if (remoteMessage.getData().containsKey("title")) {
            title = remoteMessage.getData().get("title");
        }

        if (remoteMessage.getData().containsKey("body")) {
            message = remoteMessage.getData().get("body");
        }

        showNotification(title, message);
    }

    private void showNotification(String title, String message) {

        createNotificationChannel();

        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(this).notify(
                    (int) System.currentTimeMillis(),
                    builder.build()
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted", e);
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Project Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(
                    "SPONDON project update notifications"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
