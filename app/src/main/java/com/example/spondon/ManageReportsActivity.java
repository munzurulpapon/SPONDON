
package com.example.spondon;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ManageReportsActivity extends AppCompatActivity {

    TextView tvReportCount, tvNoReports;
    ProgressBar progressReports;
    ListView lvReports;
    Button btnBackReports;

    FirebaseFirestore firestore;
    FirebaseAuth firebaseAuth;

    ArrayList<String> reportList;
    ArrayList<String> reportIds;

    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_reports);

        tvReportCount = findViewById(R.id.tvReportCount);
        tvNoReports = findViewById(R.id.tvNoReports);
        progressReports = findViewById(R.id.progressReports);
        lvReports = findViewById(R.id.lvReports);
        btnBackReports = findViewById(R.id.btnBackReports);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        reportList = new ArrayList<>();
        reportIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                reportList
        );

        lvReports.setAdapter(adapter);

        btnBackReports.setOnClickListener(v -> finish());

        lvReports.setOnItemClickListener((parent, view, position, id) -> {

            String selectedReportId = reportIds.get(position);

            Intent intent = new Intent(
                    ManageReportsActivity.this,
                    ReportAdminDetailsActivity.class
            );

            intent.putExtra("REPORT_ID", selectedReportId);
            startActivity(intent);
        });

        loadReports();
    }

    private void loadReports() {

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        progressReports.setVisibility(View.VISIBLE);
        tvNoReports.setVisibility(View.GONE);
        lvReports.setVisibility(View.GONE);

        firestore.collection("reports")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    progressReports.setVisibility(View.GONE);

                    reportList.clear();
                    reportIds.clear();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        String category = document.getString("category");
                        String title = document.getString("title");
                        String description = document.getString("description");
                        String location = document.getString("location");
                        String status = document.getString("status");

                        if (category == null) category = "Unknown";
                        if (title == null) title = "No title";
                        if (description == null) description = "No description";
                        if (location == null) location = "Unknown";
                        if (status == null) status = "Unknown";

                        String reportDetails =
                                "Title: " + title +
                                        "\nCategory: " + category +
                                        "\nDescription: " + description +
                                        "\nLocation: " + location +
                                        "\nStatus: " + status +
                                        "\n\nTap to manage this report";

                        reportList.add(reportDetails);
                        reportIds.add(document.getId());
                    }

                    tvReportCount.setText(
                            "Total Reports: " + reportList.size()
                    );

                    adapter.notifyDataSetChanged();

                    if (reportList.isEmpty()) {
                        tvNoReports.setVisibility(View.VISIBLE);
                        lvReports.setVisibility(View.GONE);
                    } else {
                        tvNoReports.setVisibility(View.GONE);
                        lvReports.setVisibility(View.VISIBLE);
                    }

                })
                .addOnFailureListener(e -> {

                    progressReports.setVisibility(View.GONE);

                    Toast.makeText(
                            ManageReportsActivity.this,
                            "Failed to load reports: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (firestore != null && firebaseAuth != null) {
            loadReports();
        }
    }
}
