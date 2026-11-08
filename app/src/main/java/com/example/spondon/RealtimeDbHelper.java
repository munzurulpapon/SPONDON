package com.example.spondon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public final class RealtimeDbHelper {

    // Leave null to use the URL from google-services.json.
    // If your database is not in us-central1, paste its URL here, e.g.
    // "https://spondon-a07ec-default-rtdb.asia-southeast1.firebasedatabase.app"
    private static final String DB_URL =
            "https://spondon-a07ec-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private RealtimeDbHelper() {}

    private static FirebaseDatabase db() {
        return DB_URL == null ? FirebaseDatabase.getInstance()
                : FirebaseDatabase.getInstance(DB_URL);
    }

    private static DatabaseReference ref(String path) {
        return db().getReference(path);
    }

    /** Mark current user online; auto-offline when connection drops. */
    public static void goOnline(@Nullable String role) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            String r = role == null ? "Citizen" : role;
            DatabaseReference p = ref("presence").child(user.getUid());

            Map<String, Object> on = new HashMap<>();
            on.put("online", true);
            on.put("role", r);
            on.put("lastSeen", ServerValue.TIMESTAMP);

            Map<String, Object> off = new HashMap<>();
            off.put("online", false);
            off.put("role", r);
            off.put("lastSeen", ServerValue.TIMESTAMP);

            p.onDisconnect().updateChildren(off);
            p.updateChildren(on);
        } catch (Exception ignored) { }
    }

    /** Call BEFORE FirebaseAuth.signOut(). */
    public static void goOffline() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            DatabaseReference p = ref("presence").child(user.getUid());
            p.onDisconnect().cancel();
            Map<String, Object> off = new HashMap<>();
            off.put("online", false);
            off.put("lastSeen", ServerValue.TIMESTAMP);
            p.updateChildren(off);
        } catch (Exception ignored) { }
    }

    /** Mirror a new emergency (key = Firestore document id). */
    public static void publishEmergency(String emergencyId, Map<String, Object> emergency) {
        try {
            if (emergencyId == null) return;
            Map<String, Object> data = new HashMap<>();
            for (String k : new String[]{"uid", "name", "phone", "type", "location",
                    "latitude", "longitude", "description", "status"}) {
                if (emergency.get(k) != null) data.put(k, emergency.get(k));
            }
            data.put("createdAt", ServerValue.TIMESTAMP);
            data.put("updatedAt", ServerValue.TIMESTAMP);
            ref("live_emergencies").child(emergencyId).setValue(data);
        } catch (Exception ignored) { }
    }

    /** Push live status / admin note changes. */
    public static void updateEmergencyStatus(String emergencyId, String status, String adminNote) {
        try {
            if (emergencyId == null) return;
            Map<String, Object> u = new HashMap<>();
            u.put("status", status);
            u.put("adminNote", adminNote == null ? "" : adminNote);
            u.put("updatedAt", ServerValue.TIMESTAMP);
            ref("live_emergencies").child(emergencyId).updateChildren(u);
        } catch (Exception ignored) { }
    }

    public interface StatusListener {
        void onStatus(String status, String adminNote);
    }

    /** Optional: live status listener for one emergency. */
    @Nullable
    public static ValueEventListener listenEmergencyStatus(String emergencyId, StatusListener l) {
        try {
            ValueEventListener vel = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot s) {
                    l.onStatus(s.child("status").getValue(String.class),
                            s.child("adminNote").getValue(String.class));
                }
                @Override public void onCancelled(@NonNull DatabaseError e) { }
            };
            ref("live_emergencies").child(emergencyId).addValueEventListener(vel);
            return vel;
        } catch (Exception e) { return null; }
    }

    public static void stopListening(String emergencyId, @Nullable ValueEventListener vel) {
        try {
            if (vel != null) ref("live_emergencies").child(emergencyId).removeEventListener(vel);
        } catch (Exception ignored) { }
    }

    public interface CountListener {
        void onCount(int onlineUsers);
    }

    /** Optional: live count of online users. */
    @Nullable
    public static ValueEventListener listenOnlineCount(CountListener l) {
        try {
            ValueEventListener vel = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot s) {
                    int n = 0;
                    for (DataSnapshot c : s.getChildren()) {
                        if (Boolean.TRUE.equals(c.child("online").getValue(Boolean.class))) n++;
                    }
                    l.onCount(n);
                }
                @Override public void onCancelled(@NonNull DatabaseError e) { }
            };
            ref("presence").addValueEventListener(vel);
            return vel;
        } catch (Exception e) { return null; }
    }
}