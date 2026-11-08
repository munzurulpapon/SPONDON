package com.example.spondon;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        handler.postDelayed(this::checkSessionAndNavigate, 2000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void checkSessionAndNavigate() {
        if (isFinishing() || isDestroyed()) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Fetch user role for direct dashboard routing
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (document != null && document.exists()) {
                        String role = document.getString("role");
                        if ("Admin".equals(role)) {
                            startActivity(new Intent(SplashActivity.this, AdminDashboardActivity.class));
                        } else {
                            Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
                            intent.putExtra("USER_ROLE", role != null ? role : "Citizen");
                            startActivity(intent);
                        }
                    } else {
                        // Fallback to Citizen dashboard if profile doc is slow/missing
                        Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
                        intent.putExtra("USER_ROLE", "Citizen");
                        startActivity(intent);
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    // Offline fallback: go to Dashboard
                    Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
                    intent.putExtra("USER_ROLE", "Citizen");
                    startActivity(intent);
                    finish();
                });
    }
}
