package com.example.spondon;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin -> Monitor Citizen Service Requests.
 *
 * Reads the whole "service_requests" collection (citizens create the documents in
 * BookServiceActivity, providers update them in ProviderRequestsActivity).
 *
 * Differences from the previous version:
 *  - No orderBy("createdAt"): that query silently drops every document without a createdAt and
 *    is the first thing to break when rules/indexes differ. Newest-first sorting is done here.
 *  - Every field is read through FirestoreUi, so one malformed document can no longer break or
 *    crash the whole list.
 *  - The real failure reason (e.g. PERMISSION_DENIED) is shown on screen with a Retry button
 *    instead of a generic toast, and the listener is re-registered on retry (Firestore cancels a
 *    listener once it has reported an error).
 */
public class AdminServiceRequestsActivity extends AppCompatActivity {

    private static final String TAG = "AdminServiceRequests";

    private TextView tvAdminMonitorCount, tvNoAdminRequests;
    private ProgressBar progressAdminServiceRequests;
    private LinearLayout layoutAdminRequestsContainer;
    private Button btnBackAdminMonitor, btnRetryAdminRequests;

    private FirebaseFirestore firestore;
    private ListenerRegistration monitorListener;

    private final List<AdminJobRecord> records = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_service_requests);

        firestore = FirebaseFirestore.getInstance();

        tvAdminMonitorCount = findViewById(R.id.tvAdminMonitorCount);
        tvNoAdminRequests = findViewById(R.id.tvNoAdminRequests);
        progressAdminServiceRequests = findViewById(R.id.progressAdminServiceRequests);
        layoutAdminRequestsContainer = findViewById(R.id.layoutAdminRequestsContainer);
        btnBackAdminMonitor = findViewById(R.id.btnBackAdminMonitor);
        btnRetryAdminRequests = findViewById(R.id.btnRetryAdminRequests);

        btnBackAdminMonitor.setOnClickListener(v -> finish());
        btnRetryAdminRequests.setOnClickListener(v -> loadAllRequests());

        loadAllRequests();
    }

    private void loadAllRequests() {
        // (Re)start cleanly: drop any previous listener and hide the previous error state.
        if (monitorListener != null) {
            monitorListener.remove();
            monitorListener = null;
        }
        btnRetryAdminRequests.setVisibility(View.GONE);
        tvNoAdminRequests.setVisibility(View.GONE);
        progressAdminServiceRequests.setVisibility(View.VISIBLE);

        if (!FirestoreUi.isOnline(this)) {
            // Keep listening anyway: Firestore will deliver data as soon as the device reconnects.
            Toast.makeText(this, "No internet connection. Waiting for the network...",
                    Toast.LENGTH_SHORT).show();
        }

        monitorListener = firestore.collection("service_requests")
                .addSnapshotListener((value, error) -> {
                    progressAdminServiceRequests.setVisibility(View.GONE);

                    if (error != null || value == null) {
                        Log.w(TAG, "service_requests listener failed", error);
                        showLoadError(FirestoreUi.describe(error));
                        return;
                    }

                    final List<AdminJobRecord> loaded = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            AdminJobRecord rec = new AdminJobRecord();
                            rec.id = doc.getId();
                            rec.serviceName = FirestoreUi.str(doc, "serviceName", "Service Request");
                            rec.category = FirestoreUi.str(doc, "category", "General");
                            rec.citizenName = FirestoreUi.str(doc, "citizenName", "Unknown citizen");
                            rec.citizenPhone = FirestoreUi.str(doc, "citizenPhone", "no phone");
                            rec.address = FirestoreUi.str(doc, "address", "not provided");
                            rec.preferredDate = FirestoreUi.str(doc, "preferredDate", "not set");
                            rec.preferredTime = FirestoreUi.str(doc, "preferredTime", null);
                            rec.providerName = FirestoreUi.str(doc, "providerName", null);
                            rec.status = FirestoreUi.str(doc, "status", "Pending");
                            rec.rating = FirestoreUi.intOf(doc, "rating");
                            rec.feedback = FirestoreUi.str(doc, "feedback", null);
                            rec.createdAt = FirestoreUi.millis(doc, "createdAt");
                            loaded.add(rec);
                        } catch (Exception e) {
                            Log.w(TAG, "Skipped unreadable request " + doc.getId(), e);
                        }
                    }

                    // Newest first; requests without a createdAt sink to the bottom instead of
                    // disappearing (which is what orderBy() used to do to them).
                    Collections.sort(loaded, (a, b) -> Long.compare(b.createdAt, a.createdAt));

                    records.clear();
                    records.addAll(loaded);
                    displayRecords(value.getMetadata().isFromCache());
                });
    }

    private void showLoadError(String reason) {
        records.clear();
        layoutAdminRequestsContainer.removeAllViews();
        tvAdminMonitorCount.setText("Could not load service requests");
        tvNoAdminRequests.setText(reason);
        tvNoAdminRequests.setVisibility(View.VISIBLE);
        btnRetryAdminRequests.setVisibility(View.VISIBLE);
    }

    private void displayRecords(boolean fromCache) {
        layoutAdminRequestsContainer.removeAllViews();
        btnRetryAdminRequests.setVisibility(View.GONE);
        tvAdminMonitorCount.setText("City-wide Service Requests (" + records.size() + ")"
                + (fromCache ? " - offline copy" : ""));

        if (records.isEmpty()) {
            tvNoAdminRequests.setText(fromCache
                    ? "No saved requests on this device yet. Connect to the internet to load them."
                    : "No service requests registered in the city yet.");
            tvNoAdminRequests.setVisibility(View.VISIBLE);
            return;
        }

        tvNoAdminRequests.setVisibility(View.GONE);

        for (AdminJobRecord rec : records) {
            addAdminCard(rec);
        }
    }

    private void addAdminCard(AdminJobRecord rec) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.parseColor("#CBD5E1"));
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(rec.serviceName + " (" + rec.category + ")");
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        StringBuilder details = new StringBuilder();
        details.append("Citizen: ").append(rec.citizenName)
                .append(" (").append(rec.citizenPhone).append(")")
                .append("\nAddress: ").append(rec.address)
                .append("\nDate: ").append(rec.preferredDate);
        if (rec.preferredTime != null) details.append(" • ").append(rec.preferredTime);
        details.append("\nProvider: ").append(rec.providerName != null ? rec.providerName : "Not assigned yet");

        TextView info = new TextView(this);
        info.setText(details.toString());
        info.setTextSize(13);
        info.setTextColor(Color.parseColor("#475569"));
        info.setPadding(0, dp(4), 0, dp(6));
        card.addView(info);

        TextView status = new TextView(this);
        status.setText("Status: " + rec.status.toUpperCase() + (rec.rating > 0 ? " • Rated: " + rec.rating + " ★" : ""));
        status.setTextSize(13);
        status.setTypeface(null, Typeface.BOLD);
        status.setTextColor(Color.parseColor("#0369A1"));
        card.addView(status);

        if (rec.feedback != null && !rec.feedback.isEmpty()) {
            TextView fb = new TextView(this);
            fb.setText("Client Review: \"" + rec.feedback + "\"");
            fb.setTextSize(12);
            fb.setTextColor(Color.parseColor("#B45309"));
            card.addView(fb);
        }

        Button btnManage = new Button(this);
        btnManage.setText("Admin Action / Override Status");
        btnManage.setTextSize(12);
        btnManage.setOnClickListener(v -> showAdminOverrideDialog(rec));
        card.addView(btnManage);

        layoutAdminRequestsContainer.addView(card);
    }

    private void showAdminOverrideDialog(AdminJobRecord rec) {
        String[] statuses = {"Pending", "Accepted", "In Progress", "Completed", "Cancelled"};

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Admin Override")
                .setMessage("Update municipal service status:")
                .setView(spinner)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Update", (d, which) -> {
                    String newStatus = spinner.getSelectedItem().toString();
                    Map<String, Object> update = new HashMap<>();
                    update.put("status", newStatus);
                    update.put("updatedAt", FieldValue.serverTimestamp());

                    firestore.collection("service_requests").document(rec.id)
                            .update(update)
                            .addOnSuccessListener(unused -> Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Could not update status: " + FirestoreUi.describe(e),
                                    Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (monitorListener != null) {
            monitorListener.remove();
        }
        super.onDestroy();
    }

    private static class AdminJobRecord {
        String id, serviceName, category, citizenName, citizenPhone, address,
                preferredDate, preferredTime, providerName, status, feedback;
        int rating = 0;
        long createdAt = 0L;
    }
}
