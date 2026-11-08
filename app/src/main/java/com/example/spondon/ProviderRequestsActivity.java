package com.example.spondon;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderRequestsActivity extends AppCompatActivity {

    private TextView tvProviderQueueCount, tvNoProviderRequests;
    private ProgressBar progressProviderQueue;
    private LinearLayout layoutProviderRequests;
    private Button btnBackProviderQueue;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ListenerRegistration queueListener;

    private final List<ProviderJob> jobList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_requests);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvProviderQueueCount = findViewById(R.id.tvProviderQueueCount);
        tvNoProviderRequests = findViewById(R.id.tvNoProviderRequests);
        progressProviderQueue = findViewById(R.id.progressProviderQueue);
        layoutProviderRequests = findViewById(R.id.layoutProviderRequests);
        btnBackProviderQueue = findViewById(R.id.btnBackProviderQueue);

        btnBackProviderQueue.setOnClickListener(v -> finish());

        loadProviderQueue();
    }

    /** Reads any Firestore value as a String (never throws on Timestamp/Number/etc.). */
    private static String str(DocumentSnapshot doc, String field) {
        try {
            Object o = doc.get(field);
            return o == null ? null : String.valueOf(o);
        } catch (Exception e) {
            return null;
        }
    }

    private static long millis(DocumentSnapshot doc, String field) {
        try {
            Object o = doc.get(field);
            if (o instanceof com.google.firebase.Timestamp) {
                return ((com.google.firebase.Timestamp) o).toDate().getTime();
            }
            if (o instanceof Number) return ((Number) o).longValue();
        } catch (Exception ignored) { }
        return 0L;
    }

    private void loadProviderQueue() {
        progressProviderQueue.setVisibility(View.VISIBLE);

        final String myUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // No orderBy(): ordering on "createdAt" silently drops every document that lacks the
        // field and can fail on rules/index problems. We sort on the device instead.
        queueListener = firestore.collection("service_requests")
                .addSnapshotListener((value, error) -> {
                    progressProviderQueue.setVisibility(View.GONE);

                    if (error != null || value == null) {
                        String why = error != null ? error.getCode().name() : "UNKNOWN";
                        Toast.makeText(this, "Failed to load request queue (" + why
                                + "). Showing saved requests.", Toast.LENGTH_LONG).show();
                        jobList.clear();
                        loadLocalServiceRequests();
                        displayQueue();
                        return;
                    }

                    final List<ProviderJob> loaded = new ArrayList<>();
                    final Map<String, Long> created = new HashMap<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            String assigned = str(doc, "providerUid");
                            boolean unassigned = assigned == null || assigned.isEmpty() || "null".equals(assigned);
                            boolean mine = myUid != null && myUid.equals(assigned);
                            // Show requests assigned to this provider plus the open pool;
                            // hide requests another provider already took.
                            if (!unassigned && !mine) continue;

                            ProviderJob job = new ProviderJob();
                            job.id = doc.getId();
                            job.serviceName = str(doc, "serviceName");
                            job.category = str(doc, "category");
                            job.citizenName = str(doc, "citizenName");
                            job.citizenPhone = str(doc, "citizenPhone");
                            job.address = str(doc, "address");
                            job.preferredDate = str(doc, "preferredDate");
                            job.preferredTime = str(doc, "preferredTime");
                            job.notes = str(doc, "notes");
                            job.status = str(doc, "status");
                            if (job.status == null || job.status.isEmpty()) job.status = "Pending";
                            if (job.serviceName == null) job.serviceName = "Service Request";

                            created.put(job.id, millis(doc, "createdAt"));
                            loaded.add(job);
                        } catch (Exception e) {
                            android.util.Log.w("ProviderQueue", "Skipped bad request " + doc.getId(), e);
                        }
                    }

                    java.util.Collections.sort(loaded, (x, y) -> {
                        long cx = created.containsKey(x.id) ? created.get(x.id) : 0L;
                        long cy = created.containsKey(y.id) ? created.get(y.id) : 0L;
                        return Long.compare(cy, cx);
                    });

                    jobList.clear();
                    jobList.addAll(loaded);

                    if (jobList.isEmpty()) {
                        loadLocalServiceRequests();
                    }
                    displayQueue();
                });
    }

    
    private void loadLocalServiceRequests() {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            android.database.Cursor cursor = db.rawQuery(
                "SELECT id, service_name, category, address, phone, preferred_date, preferred_time, notes, status, firebase_id FROM " 
                + DatabaseHelper.TABLE_SERVICE_REQUESTS + " ORDER BY id DESC", null
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ProviderJob job = new ProviderJob();
                    long localId = cursor.getLong(0);
                    job.serviceName = cursor.getString(1);
                    job.category = cursor.getString(2);
                    job.address = cursor.getString(3);
                    job.citizenPhone = cursor.getString(4);
                    job.preferredDate = cursor.getString(5);
                    job.preferredTime = cursor.getString(6);
                    job.notes = cursor.getString(7);
                    job.status = cursor.getString(8);
                    String fid = cursor.getString(9);
                    job.id = (fid != null && !fid.isEmpty()) ? fid : String.valueOf(localId);
                    job.citizenName = "Citizen (Local)";
                    if (job.status == null) job.status = "Pending";
                    jobList.add(job);
                }
                cursor.close();
            }
        } catch (Exception ignored) {}
    }

    private void displayQueue() {
        layoutProviderRequests.removeAllViews();
        tvProviderQueueCount.setText("Client Requests in Queue: " + jobList.size());

        if (jobList.isEmpty()) {
            tvNoProviderRequests.setVisibility(View.VISIBLE);
            return;
        }

        tvNoProviderRequests.setVisibility(View.GONE);

        for (ProviderJob job : jobList) {
            try {
                addJobCard(job);
            } catch (Exception e) {
                android.util.Log.w("ProviderQueue", "Card failed for " + job.id, e);
            }
        }
    }

    private void addJobCard(ProviderJob job) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);

        // Top Status Header
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView catView = new TextView(this);
        catView.setText(job.category != null ? job.category.toUpperCase() : "GENERAL");
        catView.setTextColor(Color.parseColor("#047857"));
        catView.setTextSize(11);
        catView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        catView.setLayoutParams(catParams);

        TextView statusView = new TextView(this);
        statusView.setText(job.status.toUpperCase());
        statusView.setTextSize(12);
        statusView.setTypeface(null, Typeface.BOLD);
        statusView.setPadding(dp(8), dp(4), dp(8), dp(4));

        if ("Completed".equalsIgnoreCase(job.status)) {
            statusView.setTextColor(Color.parseColor("#15803D"));
            statusView.setBackgroundColor(Color.parseColor("#DCFCE7"));
        } else if ("Accepted".equalsIgnoreCase(job.status) || "In Progress".equalsIgnoreCase(job.status)) {
            statusView.setTextColor(Color.parseColor("#1D4ED8"));
            statusView.setBackgroundColor(Color.parseColor("#DBEAFE"));
        } else {
            statusView.setTextColor(Color.parseColor("#B45309"));
            statusView.setBackgroundColor(Color.parseColor("#FEF3C7"));
        }

        top.addView(catView);
        top.addView(statusView);
        card.addView(top);

        // Job Title
        TextView title = new TextView(this);
        title.setText(job.serviceName);
        title.setTextSize(17);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(6), 0, dp(2));
        card.addView(title);

        // Client Info
        TextView client = new TextView(this);
        client.setText("Client: " + (job.citizenName != null ? job.citizenName : "Citizen") + " • " + (job.citizenPhone != null ? job.citizenPhone : ""));
        client.setTextSize(13);
        client.setTextColor(Color.parseColor("#334155"));
        client.setPadding(0, 0, 0, dp(2));
        card.addView(client);

        TextView address = new TextView(this);
        address.setText("Address: " + (job.address != null ? job.address : "Khulna City"));
        address.setTextSize(13);
        address.setTextColor(Color.parseColor("#475569"));
        card.addView(address);

        TextView time = new TextView(this);
        time.setText("Schedule: " + (job.preferredDate != null ? job.preferredDate : "ASAP") + " (" + (job.preferredTime != null ? job.preferredTime : "") + ")");
        time.setTextSize(13);
        time.setTextColor(Color.parseColor("#475569"));
        card.addView(time);

        if (job.notes != null && !job.notes.isEmpty()) {
            TextView notes = new TextView(this);
            notes.setText("Notes: " + job.notes);
            notes.setTextSize(12);
            notes.setTextColor(Color.parseColor("#64748B"));
            notes.setPadding(0, dp(4), 0, dp(4));
            card.addView(notes);
        }

        // Action Buttons Row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, 0);

        Button btnCall = new Button(this);
        btnCall.setText("Call Client");
        btnCall.setTextSize(12);
        btnCall.setOnClickListener(v -> {
            if (job.citizenPhone != null && !job.citizenPhone.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + job.citizenPhone));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Client phone number missing.", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnStatus = new Button(this);
        btnStatus.setText("Update Status");
        btnStatus.setTextSize(12);
        btnStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#059669")));
        btnStatus.setTextColor(Color.WHITE);
        btnStatus.setOnClickListener(v -> showStatusUpdateDialog(job));

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnCall.setLayoutParams(btnParams);
        btnStatus.setLayoutParams(btnParams);

        actions.addView(btnCall);
        actions.addView(btnStatus);
        card.addView(actions);

        layoutProviderRequests.addView(card);
    }

    private void showStatusUpdateDialog(ProviderJob job) {
        String[] statuses = {"Accepted", "In Progress", "Completed", "Cancelled"};

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Update Request Status")
                .setMessage("Select current job fulfillment status:")
                .setView(spinner)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Update", (d, which) -> {
                    String newStatus = spinner.getSelectedItem().toString();
                    Map<String, Object> update = new HashMap<>();
                    update.put("status", newStatus);
                    update.put("updatedAt", FieldValue.serverTimestamp());
                    if (auth.getCurrentUser() != null) {
                        update.put("providerUid", auth.getCurrentUser().getUid());
                        String pn = auth.getCurrentUser().getDisplayName();
                        if (pn == null || pn.isEmpty()) pn = auth.getCurrentUser().getEmail();
                        if (pn != null && !pn.isEmpty()) update.put("providerName", pn);
                    }

                    firestore.collection("service_requests").document(job.id)
                            .update(update)
                            .addOnSuccessListener(unused -> Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (queueListener != null) {
            queueListener.remove();
        }
        super.onDestroy();
    }

    private static class ProviderJob {
        String id, serviceName, category, citizenName, citizenPhone, address, preferredDate, preferredTime, notes, status;
    }
}
