package com.example.spondon;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvAdminTitle;
    private TextView tvTotalUsersCount, tvTotalReportsCount, tvPendingIssuesCount, tvEmergenciesCount;

    private Button btnAdminLogout;
    private Button btnManageUsers;
    private Button btnManageReports;
    private Button btnManageServices;
    private Button btnAdminServiceRequests;
    private Button btnManageEmergencies;
    private Button btnManageProjectUpdates;
    private Button btnManageJobs;
    private Button btnAdminIntelligence;
    private DatabaseHelper databaseHelper;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Register FCM Token
        MyFirebaseMessagingService.registerToken(this);
        requestNotificationPermission();

        firestore = FirebaseFirestore.getInstance();

        // Realtime Database: mark admin online (auto-offline on disconnect)
        RealtimeDbHelper.goOnline("Admin");

        tvAdminTitle = findViewById(R.id.tvAdminTitle);
        tvTotalUsersCount = findViewById(R.id.tvTotalUsersCount);
        tvTotalReportsCount = findViewById(R.id.tvTotalReportsCount);
        tvPendingIssuesCount = findViewById(R.id.tvPendingIssuesCount);
        tvEmergenciesCount = findViewById(R.id.tvEmergenciesCount);

        btnAdminLogout = findViewById(R.id.btnAdminLogout);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnManageReports = findViewById(R.id.btnManageReports);
        btnManageServices = findViewById(R.id.btnManageServices);
        btnAdminServiceRequests = findViewById(R.id.btnAdminServiceRequests);
        btnManageEmergencies = findViewById(R.id.btnManageEmergencies);
        btnManageProjectUpdates = findViewById(R.id.btnManageProjectUpdates);
        btnManageJobs = findViewById(R.id.btnManageJobs);
        btnAdminIntelligence = findViewById(R.id.btnAdminIntelligence);
        databaseHelper = new DatabaseHelper(this);

        if (btnAdminIntelligence != null) {
            btnAdminIntelligence.setOnClickListener(v -> {
                startActivity(new Intent(AdminDashboardActivity.this, CityIntelligenceActivity.class));
            });
        }

        tvAdminTitle.setText("SPONDON Admin Dashboard");

        btnManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageUsersActivity.class));
        });

        btnManageReports.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageReportsActivity.class));
        });

        btnManageServices.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageServicesActivity.class));
        });

        btnAdminServiceRequests.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminServiceRequestsActivity.class));
        });

        btnManageEmergencies.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageEmergenciesActivity.class));
        });

        btnManageProjectUpdates.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageProjectUpdatesActivity.class));
        });

        btnManageJobs.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageJobsActivity.class));
        });

        btnAdminLogout.setOnClickListener(v -> {
            RealtimeDbHelper.goOffline();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        loadDashboardMetrics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardMetrics();
    }

    private void loadDashboardMetrics() {
        // Load initial counts from local SQLite database immediately
        loadLocalMetrics();

        // Query Firestore to merge cloud data
        firestore.collection("users").get().addOnSuccessListener(snapshot -> {
            if (snapshot != null) {
                tvTotalUsersCount.setText("TOTAL CITIZENS\n\n" + snapshot.size());
            }
        });

        firestore.collection("reports").get().addOnSuccessListener(snapshot -> {
            if (snapshot != null && !snapshot.isEmpty()) {
                tvTotalReportsCount.setText("TOTAL REPORTS\n\n" + snapshot.size());
                int pending = 0;
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    String status = doc.getString("status");
                    if ("Pending".equalsIgnoreCase(status) || "Submitted".equalsIgnoreCase(status)) {
                        pending++;
                    }
                }
                tvPendingIssuesCount.setText("PENDING ISSUES\n\n" + pending);
            }
        });

        firestore.collection("emergencies").get().addOnSuccessListener(snapshot -> {
            if (snapshot != null && !snapshot.isEmpty()) {
                tvEmergenciesCount.setText("EMERGENCIES\n\n" + snapshot.size());
            }
        });
    }

    private void loadLocalMetrics() {
        if (databaseHelper == null) return;
        try {
            android.database.sqlite.SQLiteDatabase db = databaseHelper.getReadableDatabase();

            // Reports count
            int reportsTotal = 0;
            int pendingTotal = 0;
            android.database.Cursor cursor = db.rawQuery("SELECT status FROM " + DatabaseHelper.TABLE_REPORTS, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    reportsTotal++;
                    String st = cursor.getString(0);
                    if ("Pending".equalsIgnoreCase(st) || "Submitted".equalsIgnoreCase(st)) {
                        pendingTotal++;
                    }
                }
                cursor.close();
            }
            tvTotalReportsCount.setText("TOTAL REPORTS\n\n" + reportsTotal);
            tvPendingIssuesCount.setText("PENDING ISSUES\n\n" + pendingTotal);

            // Emergencies count
            int emergenciesTotal = 0;
            android.database.Cursor eCursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_EMERGENCY_REQUESTS, null);
            if (eCursor != null) {
                if (eCursor.moveToFirst()) {
                    emergenciesTotal = eCursor.getInt(0);
                }
                eCursor.close();
            }
            tvEmergenciesCount.setText("EMERGENCIES\n\n" + emergenciesTotal);

            // Default citizens if users not yet loaded
            tvTotalUsersCount.setText("TOTAL CITIZENS\n\n1");
        } catch (Exception ignored) {}
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            String postNotification = "android.permission.POST_NOTIFICATIONS";
            if (ContextCompat.checkSelfPermission(this, postNotification)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{postNotification}, 101);
            }
        }
    }
}