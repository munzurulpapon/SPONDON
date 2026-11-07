
package com.example.spondon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    TextView tvRole;

    Button btnLogout;
    Button btnCityMap, btnReportIssue;
    Button btnTransport, btnHealthcare;
    Button btnUtility, btnHomeServices;
    Button btnBloodDonation, btnEmergency;
    Button btnMyReports, btnMyEmergencyRequests;
    Button btnCommunity;
    Button btnPublicProjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register FCM Token for both Citizen and Admin
        MyFirebaseMessagingService.registerToken(this);

        String role = getIntent().getStringExtra("USER_ROLE");

        if (role == null) {
            role = "Citizen";
        }

        // Admin Dashboard
        if (role.equals("Admin")) {

            Intent intent = new Intent(
                    DashboardActivity.this,
                    AdminDashboardActivity.class
            );

            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        tvRole = findViewById(R.id.tvRole);
        btnLogout = findViewById(R.id.btnLogout);

        btnCityMap = findViewById(R.id.btnCityMap);
        btnReportIssue = findViewById(R.id.btnReportIssue);

        btnTransport = findViewById(R.id.btnTransport);
        btnHealthcare = findViewById(R.id.btnHealthcare);

        btnUtility = findViewById(R.id.btnUtility);
        btnHomeServices = findViewById(R.id.btnHomeServices);

        btnBloodDonation = findViewById(R.id.btnBloodDonation);
        btnEmergency = findViewById(R.id.btnEmergency);

        btnMyReports = findViewById(R.id.btnMyReports);
        btnMyEmergencyRequests = findViewById(R.id.btnMyEmergencyRequests);

        btnCommunity = findViewById(R.id.btnCommunity);

        btnPublicProjects = findViewById(R.id.btnPublicProjects);

        tvRole.setText(role + " Dashboard");

        // Logout
        btnLogout.setOnClickListener(v -> logout());

        // City Map
        btnCityMap.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    CityMapActivity.class
            );

            startActivity(intent);
        });

        // Report Issue
        btnReportIssue.setOnClickListener(v -> {
            startActivity(new Intent(
                    DashboardActivity.this,
                    ReportActivity.class
            ));
        });

        // Public Projects
        btnPublicProjects.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    PublicProjectsActivity.class
            );

            startActivity(intent);
        });

        // Coming Soon Services
        setServiceClick(btnTransport, "Public Transport");
        setServiceClick(btnHealthcare, "Healthcare");
        setServiceClick(btnUtility, "Utility Services");
        setServiceClick(btnHomeServices, "Home Services");
        setServiceClick(btnBloodDonation, "Blood Donation");

        // Emergency SOS
        btnEmergency.setOnClickListener(v -> {
            startActivity(new Intent(
                    DashboardActivity.this,
                    EmergencyRequestActivity.class
            ));
        });

        // My Emergency Requests
        btnMyEmergencyRequests.setOnClickListener(v -> {
            startActivity(new Intent(
                    DashboardActivity.this,
                    MyEmergencyRequestsActivity.class
            ));
        });

        // My Reports
        btnMyReports.setOnClickListener(v -> {
            startActivity(new Intent(
                    DashboardActivity.this,
                    MyReportsActivity.class
            ));
        });

        // Community
        setServiceClick(btnCommunity, "Community");
    }

    private void logout() {

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(
                DashboardActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void setServiceClick(Button button, String serviceName) {

        button.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    serviceName + " - Coming Soon!",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}
