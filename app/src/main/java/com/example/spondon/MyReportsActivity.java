
package com.example.spondon;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MyReportsActivity extends AppCompatActivity {

    TextView tvReportCount, tvEmptyReports;
    EditText etSearchReports;
    Spinner spReportStatusFilter;
    ListView lvReports;

    DatabaseHelper databaseHelper;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    ArrayList<String> reportList;
    ArrayList<Integer> reportIds;

    ArrayAdapter<String> reportAdapter;

    boolean isSyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        tvReportCount = findViewById(R.id.tvReportCount);
        tvEmptyReports = findViewById(R.id.tvEmptyReports);
        etSearchReports = findViewById(R.id.etSearchReports);
        spReportStatusFilter = findViewById(R.id.spReportStatusFilter);
        lvReports = findViewById(R.id.lvReports);

        databaseHelper = new DatabaseHelper(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        reportList = new ArrayList<>();
        reportIds = new ArrayList<>();

        reportAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                reportList
        );

        lvReports.setAdapter(reportAdapter);

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

        statusAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spReportStatusFilter.setAdapter(statusAdapter);

        etSearchReports.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                loadReports();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        spReportStatusFilter.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {
                        loadReports();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                });

        lvReports.setOnItemClickListener((parent, view, position, id) -> {

            int reportId = reportIds.get(position);

            Intent intent = new Intent(
                    MyReportsActivity.this,
                    ReportDetailsActivity.class
            );

            intent.putExtra("REPORT_ID", reportId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncFirebaseStatuses();
    }

    private void syncFirebaseStatuses() {

        if (isSyncing) {
            return;
        }

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

                    SQLiteDatabase db =
                            databaseHelper.getWritableDatabase();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        String firebaseId = document.getId();
                        String status = document.getString("status");

                        if (status == null) {
                            continue;
                        }

                        ContentValues values = new ContentValues();
                        values.put("status", status);

                        db.update(
                                DatabaseHelper.TABLE_REPORTS,
                                values,
                                "firebase_id = ?",
                                new String[]{firebaseId}
                        );
                    }

                    db.close();

                    isSyncing = false;
                    loadReports();
                })
                .addOnFailureListener(e -> {

                    isSyncing = false;
                    loadReports();

                    Toast.makeText(
                            this,
                            "Status sync failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadReports() {

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        reportList.clear();
        reportIds.clear();

        try {

            String searchText = etSearchReports.getText()
                    .toString().trim();

            String selectedStatus =
                    spReportStatusFilter.getSelectedItem() == null
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

            cursor = db.rawQuery(
                    query,
                    args.toArray(new String[0])
            );

            while (cursor.moveToNext()) {

                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id")
                );

                String title = cursor.getString(
                        cursor.getColumnIndexOrThrow("title")
                );

                String category = cursor.getString(
                        cursor.getColumnIndexOrThrow("category")
                );

                String location = cursor.getString(
                        cursor.getColumnIndexOrThrow("location")
                );

                String status = cursor.getString(
                        cursor.getColumnIndexOrThrow("status")
                );

                String report =
                        "Report ID: " + id +
                                "\nTitle: " + title +
                                "\nCategory: " + category +
                                "\nLocation: " + location +
                                "\nStatus: " + status;

                reportIds.add(id);
                reportList.add(report);
            }

            reportAdapter.notifyDataSetChanged();

            tvReportCount.setText(
                    "Total Reports: " + reportList.size()
            );

            if (reportList.isEmpty()) {

                tvEmptyReports.setVisibility(TextView.VISIBLE);
                lvReports.setVisibility(ListView.GONE);

                tvEmptyReports.setText("No matching reports found.");

            } else {

                tvEmptyReports.setVisibility(TextView.GONE);
                lvReports.setVisibility(ListView.VISIBLE);
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error loading reports: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
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
