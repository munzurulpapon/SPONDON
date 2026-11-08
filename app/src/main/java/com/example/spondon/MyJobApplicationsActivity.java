package com.example.spondon;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MyJobApplicationsActivity extends AppCompatActivity {

    private TextView tvAppCount, tvNoApps;
    private ProgressBar progressMyApps;
    private LinearLayout layoutAppsContainer;
    private Button btnBackMyApps;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private DatabaseHelper databaseHelper;
    private ListenerRegistration appsListener;

    private final List<ApplicationRecord> appList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_job_applications);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(this);

        tvAppCount = findViewById(R.id.tvAppCount);
        tvNoApps = findViewById(R.id.tvNoApps);
        progressMyApps = findViewById(R.id.progressMyApps);
        layoutAppsContainer = findViewById(R.id.layoutAppsContainer);
        btnBackMyApps = findViewById(R.id.btnBackMyApps);

        btnBackMyApps.setOnClickListener(v -> finish());

        loadApplications();
    }

    private void loadApplications() {
        if (auth.getCurrentUser() == null) {
            loadFromSQLite();
            return;
        }

        progressMyApps.setVisibility(View.VISIBLE);
        String uid = auth.getCurrentUser().getUid();

        appsListener = firestore.collection("job_applications")
                .whereEqualTo("applicantUid", uid)
                .addSnapshotListener((value, error) -> {
                    progressMyApps.setVisibility(View.GONE);

                    if (error != null || value == null) {
                        loadFromSQLite();
                        return;
                    }

                    appList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ApplicationRecord ar = new ApplicationRecord();
                        ar.id = doc.getId();
                        ar.jobTitle = doc.getString("jobTitle");
                        ar.company = doc.getString("company");
                        ar.name = doc.getString("applicantName");
                        ar.phone = doc.getString("applicantPhone");
                        ar.education = doc.getString("education");
                        ar.status = doc.getString("status");

                        if (ar.status == null) ar.status = "Submitted";
                        if (ar.jobTitle == null) ar.jobTitle = "Position";
                        appList.add(ar);
                    }

                    displayApplications();
                });
    }

    private void loadFromSQLite() {
        appList.clear();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_JOB_APPLICATIONS + " ORDER BY id DESC", null);

        while (c.moveToNext()) {
            ApplicationRecord ar = new ApplicationRecord();
            ar.jobTitle = c.getString(c.getColumnIndexOrThrow("job_title"));
            ar.company = c.getString(c.getColumnIndexOrThrow("company"));
            ar.name = c.getString(c.getColumnIndexOrThrow("applicant_name"));
            ar.phone = c.getString(c.getColumnIndexOrThrow("applicant_phone"));
            ar.education = c.getString(c.getColumnIndexOrThrow("qualifications"));
            ar.status = c.getString(c.getColumnIndexOrThrow("status"));
            appList.add(ar);
        }
        c.close();
        db.close();

        displayApplications();
    }

    private void displayApplications() {
        layoutAppsContainer.removeAllViews();
        tvAppCount.setText("Submitted Applications (" + appList.size() + ")");

        if (appList.isEmpty()) {
            tvNoApps.setVisibility(View.VISIBLE);
            return;
        }

        tvNoApps.setVisibility(View.GONE);

        for (ApplicationRecord ar : appList) {
            addAppCard(ar);
        }
    }

    private void addAppCard(ApplicationRecord ar) {
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

        // Header: Status Badge
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView compView = new TextView(this);
        compView.setText(ar.company != null ? ar.company.toUpperCase() : "COMPANY");
        compView.setTextColor(Color.parseColor("#475569"));
        compView.setTextSize(11);
        compView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams compParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        compView.setLayoutParams(compParams);

        TextView statusView = new TextView(this);
        statusView.setText(ar.status.toUpperCase());
        statusView.setTextSize(12);
        statusView.setTypeface(null, Typeface.BOLD);
        statusView.setPadding(dp(8), dp(4), dp(8), dp(4));

        if ("Shortlisted".equalsIgnoreCase(ar.status)) {
            statusView.setTextColor(Color.parseColor("#15803D"));
            statusView.setBackgroundColor(Color.parseColor("#DCFCE7"));
        } else if ("Under Review".equalsIgnoreCase(ar.status)) {
            statusView.setTextColor(Color.parseColor("#1D4ED8"));
            statusView.setBackgroundColor(Color.parseColor("#DBEAFE"));
        } else if ("Rejected".equalsIgnoreCase(ar.status)) {
            statusView.setTextColor(Color.parseColor("#B91C1C"));
            statusView.setBackgroundColor(Color.parseColor("#FEE2E2"));
        } else {
            statusView.setTextColor(Color.parseColor("#B45309"));
            statusView.setBackgroundColor(Color.parseColor("#FEF3C7"));
        }

        top.addView(compView);
        top.addView(statusView);
        card.addView(top);

        // Position Title
        TextView title = new TextView(this);
        title.setText(ar.jobTitle);
        title.setTextSize(17);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(6), 0, dp(2));
        card.addView(title);

        // Applicant details summary
        TextView details = new TextView(this);
        details.setText("Applicant: " + ar.name + " (" + ar.phone + ")\n" + (ar.education != null ? ar.education : ""));
        details.setTextSize(13);
        details.setTextColor(Color.parseColor("#475569"));
        card.addView(details);

        layoutAppsContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (appsListener != null) {
            appsListener.remove();
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }

    private static class ApplicationRecord {
        String id, jobTitle, company, name, phone, education, status;
    }
}
