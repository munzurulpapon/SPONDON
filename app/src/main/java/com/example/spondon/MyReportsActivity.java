package com.example.spondon;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyReportsActivity extends AppCompatActivity {

    private static final String STATE_SEARCH_TEXT = "STATE_SEARCH_TEXT";
    private static final String STATE_FILTER_POS = "STATE_FILTER_POS";

    TextView tvReportCount, tvEmptyReports;
    EditText etSearchReports;
    Spinner spReportStatusFilter;
    ListView lvReports;
    RecyclerView rvReports;

    DatabaseHelper databaseHelper;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    ArrayList<String> reportList;
    ArrayList<Integer> reportIds;
    ArrayAdapter<String> reportAdapter;

    private List<ReportItem> recyclerItemList;
    private ReportAdapter reportRecyclerAdapter;

    boolean isSyncing = false;
    private int restoredFilterPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        tvReportCount = findViewById(R.id.tvReportCount);
        tvEmptyReports = findViewById(R.id.tvEmptyReports);
        etSearchReports = findViewById(R.id.etSearchReports);
        spReportStatusFilter = findViewById(R.id.spReportStatusFilter);
        lvReports = findViewById(R.id.lvReports);
        rvReports = findViewById(R.id.rvReports);

        databaseHelper = new DatabaseHelper(this);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        reportList = new ArrayList<>();
        reportIds = new ArrayList<>();
        recyclerItemList = new ArrayList<>();

        // Initialize RecyclerView with custom ReportAdapter and ViewHolder
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        reportRecyclerAdapter = new ReportAdapter(recyclerItemList, report -> {
            Intent intent = new Intent(MyReportsActivity.this, ReportDetailsActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
        rvReports.setAdapter(reportRecyclerAdapter);

        // Fallback adapter for legacy ListView compatibility
        reportAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reportList);
        if (lvReports != null) {
            lvReports.setAdapter(reportAdapter);
        }

        View btnBack = findViewById(R.id.btnBackMyReports);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView chipAll = findViewById(R.id.chipFilterAll);
        TextView chipPending = findViewById(R.id.chipFilterPending);
        TextView chipProgress = findViewById(R.id.chipFilterProgress);
        TextView chipResolved = findViewById(R.id.chipFilterResolved);

        if (chipAll != null && chipPending != null && chipProgress != null && chipResolved != null) {
            chipAll.setOnClickListener(v -> {
                updateChipSelection(chipAll, chipPending, chipProgress, chipResolved);
                spReportStatusFilter.setSelection(0);
            });
            chipPending.setOnClickListener(v -> {
                updateChipSelection(chipPending, chipAll, chipProgress, chipResolved);
                spReportStatusFilter.setSelection(2);
            });
            chipProgress.setOnClickListener(v -> {
                updateChipSelection(chipProgress, chipAll, chipPending, chipResolved);
                spReportStatusFilter.setSelection(3);
            });
            chipResolved.setOnClickListener(v -> {
                updateChipSelection(chipResolved, chipAll, chipPending, chipProgress);
                spReportStatusFilter.setSelection(4);
            });
        }

        String[] statuses = {
                "All Reports",
                "Submitted",
                "Pending",
                "In Progress",
                "Resolved",
                "Rejected"
        };

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statuses
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReportStatusFilter.setAdapter(statusAdapter);

        // Restore screen state if rotating (Requirement 10)
        if (savedInstanceState != null) {
            String savedSearch = savedInstanceState.getString(STATE_SEARCH_TEXT, "");
            etSearchReports.setText(savedSearch);
            restoredFilterPos = savedInstanceState.getInt(STATE_FILTER_POS, 0);
            if (restoredFilterPos < statuses.length) {
                spReportStatusFilter.setSelection(restoredFilterPos);
            }
        }

        etSearchReports.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadReports();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spReportStatusFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                loadReports();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (etSearchReports != null) {
            outState.putString(STATE_SEARCH_TEXT, etSearchReports.getText().toString());
        }
        if (spReportStatusFilter != null) {
            outState.putInt(STATE_FILTER_POS, spReportStatusFilter.getSelectedItemPosition());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncOfflineReports();
        syncFirebaseStatuses();
    }

    private void syncOfflineReports() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM reports WHERE firebase_id IS NULL OR firebase_id = ''", null);

        while (cursor.moveToNext()) {
            int localId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            String location = cursor.getString(cursor.getColumnIndexOrThrow("location"));
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

            Map<String, Object> map = new HashMap<>();
            map.put("uid", uid);
            map.put("category", category != null ? category : "Other");
            map.put("title", title != null ? title : "Citizen Report");
            map.put("description", description != null ? description : "");
            map.put("location", location != null ? location : "");
            map.put("status", status != null ? status : "Submitted");
            map.put("createdAt", FieldValue.serverTimestamp());

            firestore.collection("reports").add(map).addOnSuccessListener(doc -> {
                SQLiteDatabase writeDb = databaseHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("firebase_id", doc.getId());
                writeDb.update("reports", cv, "id = ?", new String[]{String.valueOf(localId)});
                writeDb.close();
            });
        }
        cursor.close();
        db.close();
    }

    private void syncFirebaseStatuses() {
        if (isSyncing) return;

        if (firebaseAuth.getCurrentUser() == null) {
            loadReports();
            return;
        }

        isSyncing = true;
        String uid = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("reports")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String firebaseId = document.getId();
                        String status = document.getString("status");
                        String title = document.getString("title");
                        String category = document.getString("category");
                        String description = document.getString("description");
                        String location = document.getString("location");

                        if (status == null) {
                            status = "Submitted";
                        }

                        Cursor c = db.rawQuery("SELECT id FROM reports WHERE firebase_id = ?", new String[]{firebaseId});
                        if (c.moveToFirst()) {
                            ContentValues values = new ContentValues();
                            values.put("status", status);
                            db.update(DatabaseHelper.TABLE_REPORTS, values, "firebase_id = ?", new String[]{firebaseId});
                        } else {
                            ContentValues values = new ContentValues();
                            values.put("firebase_id", firebaseId);
                            values.put("title", title != null ? title : "Report");
                            values.put("category", category != null ? category : "General");
                            values.put("description", description != null ? description : "");
                            values.put("location", location != null ? location : "");
                            values.put("status", status);
                            values.put("uid", uid);
                            db.insert(DatabaseHelper.TABLE_REPORTS, null, values);
                        }
                        c.close();
                    }

                    db.close();
                    isSyncing = false;
                    loadReports();
                })
                .addOnFailureListener(e -> {
                    isSyncing = false;
                    loadReports();
                });
    }

    private void loadReports() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        reportList.clear();
        reportIds.clear();
        recyclerItemList.clear();

        try {
            String searchText = etSearchReports.getText().toString().trim();
            String selectedStatus = spReportStatusFilter.getSelectedItem() == null
                    ? "All Reports"
                    : spReportStatusFilter.getSelectedItem().toString();

            String query = "SELECT * FROM reports WHERE 1=1";
            ArrayList<String> args = new ArrayList<>();

            if (!searchText.isEmpty()) {
                query += " AND (title LIKE ? OR category LIKE ? OR location LIKE ?)";
                String searchValue = "%" + searchText + "%";
                args.add(searchValue);
                args.add(searchValue);
                args.add(searchValue);
            }

            if (!selectedStatus.equals("All Reports")) {
                query += " AND status = ?";
                args.add(selectedStatus);
            }

            query += " ORDER BY id DESC";

            cursor = db.rawQuery(query, args.toArray(new String[0]));

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                String location = cursor.getString(cursor.getColumnIndexOrThrow("location"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                int imgCol = cursor.getColumnIndex("image_uri");
                String imageUri = imgCol != -1 ? cursor.getString(imgCol) : null;

                int latCol = cursor.getColumnIndex("latitude");
                double lat = (latCol != -1 && !cursor.isNull(latCol)) ? cursor.getDouble(latCol) : 0.0;

                int lonCol = cursor.getColumnIndex("longitude");
                double lon = (lonCol != -1 && !cursor.isNull(lonCol)) ? cursor.getDouble(lonCol) : 0.0;

                int fbCol = cursor.getColumnIndex("firebase_id");
                String fbId = fbCol != -1 ? cursor.getString(fbCol) : null;

                int createdCol = cursor.getColumnIndex("created_at");
                String createdAt = createdCol != -1 ? cursor.getString(createdCol) : null;

                ReportItem item = new ReportItem(id, category, title, description, location, status,
                        imageUri, lat, lon, fbId, createdAt);
                recyclerItemList.add(item);

                reportIds.add(id);
                reportList.add("Report #" + id + ": " + title + " [" + status + "]");
            }

            reportRecyclerAdapter.notifyDataSetChanged();
            reportAdapter.notifyDataSetChanged();

            tvReportCount.setText(recyclerItemList.size() + " Reports");

            if (recyclerItemList.isEmpty()) {
                tvEmptyReports.setVisibility(View.VISIBLE);
                rvReports.setVisibility(View.GONE);
                tvEmptyReports.setText("No matching civic reports found.");
            } else {
                tvEmptyReports.setVisibility(View.GONE);
                rvReports.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error loading reports: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    private void updateChipSelection(TextView selected, TextView... unselected) {
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(Color.WHITE);
        for (TextView u : unselected) {
            u.setBackgroundResource(R.drawable.bg_chip_unselected);
            u.setTextColor(Color.parseColor("#475569"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
