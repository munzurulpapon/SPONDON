package com.example.spondon;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageView btnBack = findViewById(R.id.btnBackProfile);
        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvEmail = findViewById(R.id.tvProfileEmail);

        View rowReports = findViewById(R.id.rowProfileReports);
        View rowNotifications = findViewById(R.id.rowProfileNotifications);
        View rowRewards = findViewById(R.id.rowProfileRewards);
        View rowSettings = findViewById(R.id.rowProfileSettings);
        View rowHelp = findViewById(R.id.rowProfileHelp);
        View rowAbout = findViewById(R.id.rowProfileAbout);
        Button btnLogout = findViewById(R.id.btnProfileLogout);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                tvName.setText(user.getDisplayName());
            } else {
                tvName.setText("Citizen User");
            }
            if (user.getEmail() != null) {
                tvEmail.setText(user.getEmail());
            } else {
                tvEmail.setText("citizen@spondon.bd");
            }
        }

        if (rowReports != null) {
            rowReports.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, MyReportsActivity.class)));
        }

        if (rowNotifications != null) {
            rowNotifications.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, NotificationCenterActivity.class)));
        }

        if (rowRewards != null) {
            rowRewards.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, RewardsActivity.class)));
        }

        if (rowSettings != null) {
            rowSettings.setOnClickListener(v -> Toast.makeText(this, "Profile and notification settings are up to date.", Toast.LENGTH_SHORT).show());
        }

        if (rowHelp != null) {
            rowHelp.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, CommunicationCenterActivity.class)));
        }

        if (rowAbout != null) {
            rowAbout.setOnClickListener(v -> showAboutDialog());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About SPONDON")
                .setMessage("SPONDON — Khulna Smart City Super App Platform\n\n"
                        + "Version: 2.0 (Production Release)\n"
                        + "Built for seamless civic issue reporting, urban mobility, emergency coordination, public transparency and municipal accountability.\n\n"
                        + "Powered by SPONDON Municipal Platform.")
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void logout() {
        CivicMonitoringService.stop(this);
        RealtimeDbHelper.goOffline();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
