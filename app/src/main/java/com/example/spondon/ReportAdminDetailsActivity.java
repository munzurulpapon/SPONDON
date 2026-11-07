
package com.example.spondon;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReportAdminDetailsActivity extends AppCompatActivity {

    TextView tvDetailTitle, tvDetailCategory;
    TextView tvDetailDescription, tvDetailLocation;
    TextView tvDetailUid, tvDetailCurrentStatus;

    Spinner spReportStatus;
    EditText etAdminNote;
    Button btnUpdateStatus, btnBackDetail;

    FirebaseFirestore firestore;

    String reportId;

    String[] statuses = {
            "Submitted",
            "Pending",
            "In Progress",
            "Resolved",
            "Rejected"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_admin_details);

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailUid = findViewById(R.id.tvDetailUid);
        tvDetailCurrentStatus = findViewById(R.id.tvDetailCurrentStatus);

        spReportStatus = findViewById(R.id.spReportStatus);
        etAdminNote = findViewById(R.id.etAdminNote);

        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnBackDetail = findViewById(R.id.btnBackDetail);

        firestore = FirebaseFirestore.getInstance();

        reportId = getIntent().getStringExtra("REPORT_ID");

        if (reportId == null || reportId.isEmpty()) {
            Toast.makeText(this, "Invalid Report ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statuses
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spReportStatus.setAdapter(adapter);

        btnBackDetail.setOnClickListener(v -> finish());

        btnUpdateStatus.setOnClickListener(v -> updateReportStatus());

        loadReportDetails();
    }

    private void loadReportDetails() {

        firestore.collection("reports")
                .document(reportId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String title = document.getString("title");
                    String category = document.getString("category");
                    String description = document.getString("description");
                    String location = document.getString("location");
                    String uid = document.getString("uid");
                    String status = document.getString("status");
                    String note = document.getString("adminNote");

                    tvDetailTitle.setText(title == null ? "No title" : title);
                    tvDetailCategory.setText("Category: " + category);
                    tvDetailDescription.setText("Description: " + description);
                    tvDetailLocation.setText("Location: " + location);
                    tvDetailUid.setText("Citizen UID: " + uid);
                    tvDetailCurrentStatus.setText("Current Status: " + status);

                    if (status != null) {
                        for (int i = 0; i < statuses.length; i++) {
                            if (statuses[i].equals(status)) {
                                spReportStatus.setSelection(i);
                                break;
                            }
                        }
                    }

                    if (note != null) {
                        etAdminNote.setText(note);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void updateReportStatus() {

        String newStatus = spReportStatus.getSelectedItem().toString();
        String adminNote = etAdminNote.getText().toString().trim();

        btnUpdateStatus.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("adminNote", adminNote);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("reports")
                .document(reportId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    btnUpdateStatus.setEnabled(true);

                    tvDetailCurrentStatus.setText(
                            "Current Status: " + newStatus
                    );

                    Toast.makeText(
                            this,
                            "Report status updated successfully!",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e -> {

                    btnUpdateStatus.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
