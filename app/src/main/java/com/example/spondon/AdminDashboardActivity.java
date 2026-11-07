
package com.example.spondon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    TextView tvAdminTitle;

    Button btnAdminLogout;
    Button btnManageUsers;
    Button btnManageReports;
    Button btnManageServices;
    Button btnManageEmergencies;
    Button btnManageProjectUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin_dashboard);

        // Register FCM Token
        MyFirebaseMessagingService.registerToken(this);

        tvAdminTitle = findViewById(R.id.tvAdminTitle);

        btnAdminLogout = findViewById(R.id.btnAdminLogout);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnManageReports = findViewById(R.id.btnManageReports);
        btnManageServices = findViewById(R.id.btnManageServices);
        btnManageEmergencies = findViewById(R.id.btnManageEmergencies);
        btnManageProjectUpdates = findViewById(R.id.btnManageProjectUpdates);

        tvAdminTitle.setText("SPONDON Admin Dashboard");

        btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageUsersActivity.class
            );
            startActivity(intent);
        });

        btnManageReports.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageReportsActivity.class
            );
            startActivity(intent);
        });

        btnManageServices.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageServicesActivity.class
            );
            startActivity(intent);
        });

        btnManageEmergencies.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageEmergenciesActivity.class
            );
            startActivity(intent);
        });

        btnManageProjectUpdates.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageProjectUpdatesActivity.class
            );
            startActivity(intent);
        });

        btnAdminLogout.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }
}
