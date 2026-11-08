package com.example.spondon;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageReportsActivity extends AppCompatActivity {

    private TextView tvReportsTitle, tvReportCount, tvNoReports;
    private EditText etSearchManageReports;
    private ProgressBar progressReports;
    private LinearLayout layoutReportsContainer;
    private Button btnBackReports;

    private Button btnFilterReportAll, btnFilterReportPending, btnFilterReportProgress, btnFilterReportResolved;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;

    private final List<AdminReportItem> reportList = new ArrayList<>();
    private String selectedStatus = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_reports);

        tvReportsTitle = findViewById(R.id.tvReportsTitle);
        tvReportCount = findViewById(R.id.tvReportCount);
        tvNoReports = findViewById(R.id.tvNoReports);
        etSearchManageReports = findViewById(R.id.etSearchManageReports);
        progressReports = findViewById(R.id.progressReports);
        layoutReportsContainer = findViewById(R.id.layoutReportsContainer);
        btnBackReports = findViewById(R.id.btnBackReports);

        btnFilterReportAll = findViewById(R.id.btnFilterReportAll);
        btnFilterReportPending = findViewById(R.id.btnFilterReportPending);
        btnFilterReportProgress = findViewById(R.id.btnFilterReportProgress);
        btnFilterReportResolved = findViewById(R.id.btnFilterReportResolved);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        databaseHelper = new DatabaseHelper(this);

        btnBackReports.setOnClickListener(v -> finish());

        setupFilters();
        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }

    private void setupFilters() {
        btnFilterReportAll.setOnClickListener(v -> setFilter("All"));
        btnFilterReportPending.setOnClickListener(v -> setFilter("Pending"));
        btnFilterReportProgress.setOnClickListener(v -> setFilter("In Progress"));
        btnFilterReportResolved.setOnClickListener(v -> setFilter("Resolved"));
    }

    private void setFilter(String filter) {
        selectedStatus = filter;
        updateFilterButtonStyles();
        displayReports();
    }

    private void updateFilterButtonStyles() {
        styleFilterBtn(btnFilterReportAll, "All");
        styleFilterBtn(btnFilterReportPending, "Pending");
        styleFilterBtn(btnFilterReportProgress, "In Progress");
        styleFilterBtn(btnFilterReportResolved, "Resolved");
    }

    private void styleFilterBtn(Button btn, String filterName) {
        if (selectedStatus.equalsIgnoreCase(filterName)) {
            btn.setBackgroundColor(Color.parseColor("#1D4ED8"));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundColor(Color.parseColor("#E2E8F0"));
            btn.setTextColor(Color.parseColor("#0F172A"));
        }
    }

    private void setupSearch() {
        etSearchManageReports.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayReports();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadReports() {
        progressReports.setVisibility(View.VISIBLE);
        tvNoReports.setVisibility(View.GONE);

        // Load local SQLite reports first
        loadSqliteReports();

        // Query Firestore to merge cloud submissions
        firestore.collection("reports")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressReports.setVisibility(View.GONE);

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String id = doc.getId();
                            String title = doc.getString("title");
                            String category = doc.getString("category");
                            String description = doc.getString("description");
                            String location = doc.getString("location");
                            String status = doc.getString("status");

                            // Avoid duplicate if already loaded from SQLite
                            boolean exists = false;
                            for (AdminReportItem item : reportList) {
                                if (id.equals(item.id)) {
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {
                                reportList.add(new AdminReportItem(id, title, category, description, location, status));
                            }
                        }
                    }
                    displayReports();
                })
                .addOnFailureListener(e -> {
                    progressReports.setVisibility(View.GONE);
                    displayReports();
                });
    }

    private void loadSqliteReports() {
        reportList.clear();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, title, category, description, location, status, firebase_id FROM "
                        + DatabaseHelper.TABLE_REPORTS
                        + " ORDER BY id DESC",
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long localId = cursor.getLong(0);
                String title = cursor.getString(1);
                String category = cursor.getString(2);
                String description = cursor.getString(3);
                String location = cursor.getString(4);
                String status = cursor.getString(5);
                String firebaseId = cursor.getString(6);

                String effectiveId = (firebaseId != null && !firebaseId.isEmpty()) ? firebaseId : String.valueOf(localId);
                reportList.add(new AdminReportItem(effectiveId, title, category, description, location, status));
            }
            cursor.close();
        }
    }

    private void displayReports() {
        layoutReportsContainer.removeAllViews();

        int visibleCount = 0;

        for (AdminReportItem item : reportList) {
            boolean matchesFilter = true;
            if ("Pending".equalsIgnoreCase(selectedStatus)) {
                matchesFilter = "Pending".equalsIgnoreCase(item.status) || "Submitted".equalsIgnoreCase(item.status);
            } else if (!"All".equalsIgnoreCase(selectedStatus)) {
                matchesFilter = selectedStatus.equalsIgnoreCase(item.status);
            }

            boolean matchesSearch = searchQuery.isEmpty()
                    || (item.title != null && item.title.toLowerCase(Locale.ROOT).contains(searchQuery))
                    || (item.category != null && item.category.toLowerCase(Locale.ROOT).contains(searchQuery))
                    || (item.location != null && item.location.toLowerCase(Locale.ROOT).contains(searchQuery))
                    || (item.status != null && item.status.toLowerCase(Locale.ROOT).contains(searchQuery));

            if (matchesFilter && matchesSearch) {
                visibleCount++;
                layoutReportsContainer.addView(createReportCard(item));
            }
        }

        tvReportCount.setText("Showing " + visibleCount + " of " + reportList.size() + " reports");
        tvNoReports.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
    }

    private View createReportCard(AdminReportItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(16);
        bg.setStroke(2, Color.parseColor("#E2E8F0"));
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setPadding(32, 28, 32, 28);

        // Header Row: Category Badge + Status Badge
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvCat = new TextView(this);
        tvCat.setText((item.category != null ? item.category : "GENERAL").toUpperCase(Locale.ROOT));
        tvCat.setTextSize(11);
        tvCat.setTypeface(null, Typeface.BOLD);
        tvCat.setPadding(16, 6, 16, 6);
        tvCat.setTextColor(Color.parseColor("#1E40AF"));

        GradientDrawable catBg = new GradientDrawable();
        catBg.setColor(Color.parseColor("#DBEAFE"));
        catBg.setCornerRadius(10);
        tvCat.setBackground(catBg);

        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1.0f);
        spacer.setLayoutParams(spacerParams);

        TextView tvStatus = new TextView(this);
        String st = item.status != null ? item.status : "Pending";
        tvStatus.setText(st.toUpperCase(Locale.ROOT));
        tvStatus.setTextSize(11);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setPadding(16, 6, 16, 6);

        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(10);

        if ("Resolved".equalsIgnoreCase(st) || "Completed".equalsIgnoreCase(st)) {
            statusBg.setColor(Color.parseColor("#DCFCE7"));
            tvStatus.setTextColor(Color.parseColor("#166534"));
        } else if ("In Progress".equalsIgnoreCase(st)) {
            statusBg.setColor(Color.parseColor("#DBEAFE"));
            tvStatus.setTextColor(Color.parseColor("#1D4ED8"));
        } else if ("Rejected".equalsIgnoreCase(st)) {
            statusBg.setColor(Color.parseColor("#FEE2E2"));
            tvStatus.setTextColor(Color.parseColor("#991B1B"));
        } else {
            statusBg.setColor(Color.parseColor("#FEF3C7"));
            tvStatus.setTextColor(Color.parseColor("#92400E"));
        }
        tvStatus.setBackground(statusBg);

        topRow.addView(tvCat);
        topRow.addView(spacer);
        topRow.addView(tvStatus);
        card.addView(topRow);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(item.title != null ? item.title : "Untitled Civic Issue");
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 16, 0, 6);
        card.addView(tvTitle);

        // Location
        if (item.location != null && !item.location.isEmpty()) {
            TextView tvLoc = new TextView(this);
            tvLoc.setText("Location: " + item.location);
            tvLoc.setTextSize(12);
            tvLoc.setTextColor(Color.parseColor("#64748B"));
            tvLoc.setPadding(0, 0, 0, 8);
            card.addView(tvLoc);
        }

        // Description preview
        if (item.description != null && !item.description.isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(item.description);
            tvDesc.setTextSize(13);
            tvDesc.setTextColor(Color.parseColor("#334155"));
            tvDesc.setMaxLines(2);
            card.addView(tvDesc);
        }

        // Footer hint
        TextView tvTapHint = new TextView(this);
        tvTapHint.setText("Tap to review & update resolution status →");
        tvTapHint.setTextSize(11);
        tvTapHint.setTextColor(Color.parseColor("#2563EB"));
        tvTapHint.setTypeface(null, Typeface.BOLD);
        tvTapHint.setPadding(0, 14, 0, 0);
        card.addView(tvTapHint);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(ManageReportsActivity.this, ReportAdminDetailsActivity.class);
            intent.putExtra("REPORT_ID", item.id);
            startActivity(intent);
        });

        return card;
    }

    private static class AdminReportItem {
        String id;
        String title;
        String category;
        String description;
        String location;
        String status;

        AdminReportItem(String id, String title, String category, String description, String location, String status) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.description = description;
            this.location = location;
            this.status = status;
        }
    }
}
