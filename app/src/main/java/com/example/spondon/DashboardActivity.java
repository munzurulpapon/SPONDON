package com.example.spondon;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private SystemAlertReceiver systemAlertReceiver;

    private TextView tvRole;
    private LinearLayout layoutProviderWorkspace;

    private Button btnLogout;
    private Button btnProviderRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String role = getIntent() != null ? getIntent().getStringExtra("USER_ROLE") : null;
        if (role == null) {
            role = "Citizen";
        }

        // Admin Dashboard redirection if user has Admin role
        if ("Admin".equals(role)) {
            startActivity(new Intent(DashboardActivity.this, AdminDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        // Background work is wrapped so a Firebase / service problem can never close the app.
        startBackgroundServicesSafely(role, savedInstanceState == null);

        tvRole = findViewById(R.id.tvRole);
        btnLogout = findViewById(R.id.btnLogout);
        layoutProviderWorkspace = findViewById(R.id.layoutProviderWorkspace);
        btnProviderRequests = findViewById(R.id.btnProviderRequests);

        if (tvRole != null) {
            tvRole.setText(role + " Dashboard");
        }

        // Service Provider workspace visibility
        boolean isProvider = "Service Provider".equalsIgnoreCase(role);
        if (layoutProviderWorkspace != null) {
            layoutProviderWorkspace.setVisibility(isProvider ? View.VISIBLE : View.GONE);
        }
        if (isProvider && btnProviderRequests != null) {
            ClickGuard.bind(this, btnProviderRequests, btnProviderRequests, ProviderRequestsActivity.class);
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }

        // ---- Quick City Services grid: the WHOLE card opens on a single tap ----
        bindGridCard(R.id.btnReportIssue, ReportActivity.class);
        bindGridCard(R.id.btnEmergency, EmergencyRequestActivity.class);
        bindGridCard(R.id.btnServicesDirectory, ServicesActivity.class);
        bindGridCard(R.id.btnCityMap, CityMapActivity.class);
        bindGridCard(R.id.btnHealthcare, HealthcareActivity.class);
        bindGridCard(R.id.btnTourism, TourismActivity.class);
        bindGridCard(R.id.btnJobs, JobsActivity.class);
        bindGridCard(R.id.btnCommunity, CommunityActivity.class);
        bindGridCard(R.id.btnPublicUtilities, PublicUtilitiesActivity.class);

        // ---- Urban Governance & Welfare list ----
        bindButton(R.id.btnMyReports, MyReportsActivity.class);
        bindButton(R.id.btnTransportation, TransportationActivity.class);
        bindButton(R.id.btnDisasterManagement, DisasterManagementActivity.class);
        bindButton(R.id.btnEnvironmental, EnvironmentalActivity.class);
        bindButton(R.id.btnRewards, RewardsActivity.class);
        bindButton(R.id.btnBloodDonation, BloodDonationActivity.class);
        bindButton(R.id.btnVehicleRoad, VehicleRoadActivity.class);
        bindButton(R.id.btnPublicProjects, PublicProjectsActivity.class);
        bindButton(R.id.btnCommunicationCenter, CommunicationCenterActivity.class);
        bindButton(R.id.btnMyEmergencyRequests, MyEmergencyRequestsActivity.class);
        bindButton(R.id.btnMyServiceRequests, MyServiceRequestsActivity.class);
        bindButton(R.id.btnMyJobApplications, MyJobApplicationsActivity.class);
        bindButton(R.id.btnNotificationCenter, NotificationCenterActivity.class);

        // ---- City Pulse ("View Pulse" button + the whole Safety Score card) ----
        View btnPulse = findViewById(R.id.btnCityIntelligence);
        View cardPulse = findViewById(R.id.cardSafetyScore);
        ClickGuard.bind(this, cardPulse, btnPulse, CityIntelligenceActivity.class);

        // ---- Header & bottom navigation ----
        bindView(R.id.layoutBellFrame, NotificationCenterActivity.class);
        bindView(R.id.btnNavReports, MyReportsActivity.class);
        bindView(R.id.btnNavNotifications, NotificationCenterActivity.class);
        bindView(R.id.btnNavProfile, ProfileActivity.class);

        View btnNavHome = findViewById(R.id.btnNavHome);
        if (btnNavHome != null) {
            btnNavHome.setClickable(true);
            btnNavHome.setOnClickListener(v -> {
                View sv = findViewById(R.id.svDashboardContent);
                if (sv instanceof ScrollView) {
                    ((ScrollView) sv).smoothScrollTo(0, 0);
                }
            });
        }

        View menu = findViewById(R.id.ivDashboardMenu);
        if (menu != null) {
            menu.setClickable(true);
            menu.setOnClickListener(this::showMenu);
        }
    }

    // ------------------------------------------------------------------
    // Click wiring helpers
    // ------------------------------------------------------------------

    /** Grid card: the Button AND the card that contains it both open the screen. */
    private void bindGridCard(int buttonId, Class<?> target) {
        View button = findViewById(buttonId);
        if (button == null) return;
        ViewParent parent = button.getParent();
        View card = (parent instanceof View) ? (View) parent : null;
        ClickGuard.bind(this, card, button, target);
    }

    private void bindButton(int id, Class<?> target) {
        View v = findViewById(id);
        if (v != null) ClickGuard.bind(this, null, v, target);
    }

    private void bindView(int id, Class<?> target) {
        View v = findViewById(id);
        if (v == null) return;
        v.setClickable(true);
        ClickGuard.bind(this, null, v, target);
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "My Profile");
        popup.getMenu().add(0, 2, 1, "Notifications");
        popup.getMenu().add(0, 3, 2, "My Reports");
        popup.getMenu().add(0, 4, 3, "Logout");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: ClickGuard.open(this, ProfileActivity.class); return true;
                case 2: ClickGuard.open(this, NotificationCenterActivity.class); return true;
                case 3: ClickGuard.open(this, MyReportsActivity.class); return true;
                case 4: logout(); return true;
                default: return false;
            }
        });
        popup.show();
    }

    // ------------------------------------------------------------------
    // Crash-proof background startup
    // ------------------------------------------------------------------

    private void startBackgroundServicesSafely(String role, boolean firstCreate) {
        try {
            MyFirebaseMessagingService.registerToken(this);
        } catch (Exception e) {
            Log.w(TAG, "FCM token registration skipped", e);
        }

        try {
            requestNotificationPermission();
        } catch (Exception e) {
            Log.w(TAG, "Notification permission request skipped", e);
        }

        try {
            // Don't restart the service every time the screen rotates
            if (firstCreate && !CivicMonitoringService.isRunning) {
                CivicMonitoringService.start(this);
            }
        } catch (Exception e) {
            Log.w(TAG, "Monitoring service not started", e);
        }

        try {
            RealtimeDbHelper.goOnline(role);
        } catch (Exception e) {
            Log.w(TAG, "Presence not updated", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Dynamic registration of BroadcastReceiver for Airplane Mode & Battery events
        if (systemAlertReceiver == null) {
            try {
                systemAlertReceiver = new SystemAlertReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                filter.addAction(Intent.ACTION_BATTERY_LOW);
                filter.addAction(Intent.ACTION_BATTERY_OKAY);
                registerReceiver(systemAlertReceiver, filter);
            } catch (Exception e) {
                Log.w(TAG, "System alert receiver not registered", e);
                systemAlertReceiver = null;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (systemAlertReceiver != null) {
            try {
                unregisterReceiver(systemAlertReceiver);
            } catch (Exception ignored) {}
            systemAlertReceiver = null;
        }
    }

    private void logout() {
        try {
            CivicMonitoringService.stop(this);
            RealtimeDbHelper.goOffline();
        } catch (Exception e) {
            Log.w(TAG, "Cleanup before logout failed", e);
        }
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
