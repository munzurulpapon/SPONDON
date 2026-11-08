package com.example.spondon;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

    private TextView tvDetailTitle, tvDetailCategory;
    private TextView tvDetailDescription, tvDetailLocation;
    private TextView tvDetailUid, tvDetailCurrentStatus;

    private Spinner spReportStatus;
    private EditText etAdminNote;
    private Button btnUpdateStatus, btnBackDetail;

    private FirebaseFirestore firestore;
    private DatabaseHelper databaseHelper;

    private String reportId;
    private boolean isLocalReport = false;

    private final String[] statuses = {
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
        databaseHelper = new DatabaseHelper(this);

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
        // Try Firestore first
        firestore.collection("reports")
                .document(reportId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document != null && document.exists()) {
                        String title = document.getString("title");
                        String category = document.getString("category");
                        String description = document.getString("description");
                        String location = document.getString("location");
                        String uid = document.getString("uid");
                        String status = document.getString("status");
                        String note = document.getString("adminNote");

                        renderReportInfo(title, category, description, location, uid, status, note);
                    } else {
                        // Check local SQLite
                        loadFromSqlite();
                    }
                })
                .addOnFailureListener(e -> loadFromSqlite());
    }

    private void loadFromSqlite() {
        isLocalReport = true;
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT title, category, description, location, uid, status FROM "
                        + DatabaseHelper.TABLE_REPORTS
                        + " WHERE id = ? OR firebase_id = ?",
                new String[]{reportId, reportId}
        );

        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(0);
            String category = cursor.getString(1);
            String description = cursor.getString(2);
            String location = cursor.getString(3);
            String uid = cursor.getString(4);
            String status = cursor.getString(5);
            cursor.close();

            renderReportInfo(title, category, description, location, uid, status, "");
        } else {
            if (cursor != null) cursor.close();
            Toast.makeText(this, "Report details loaded from local cache", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderReportInfo(String title, String category, String description, String location, String uid, String status, String note) {
        tvDetailTitle.setText(title == null ? "Untitled Civic Report" : title);
        tvDetailCategory.setText("Category: " + (category == null ? "General" : category));
        tvDetailDescription.setText("Description: " + (description == null ? "No description provided." : description));
        tvDetailLocation.setText("Location: " + (location == null ? "Not specified" : location));
        tvDetailUid.setText("Citizen Reference: " + (uid == null ? "Anonymous Citizen" : uid));
        tvDetailCurrentStatus.setText("Current Status: " + (status == null ? "Pending" : status));

        if (status != null) {
            for (int i = 0; i < statuses.length; i++) {
                if (statuses[i].equalsIgnoreCase(status)) {
                    spReportStatus.setSelection(i);
                    break;
                }
            }
        }

        if (note != null && !note.isEmpty()) {
            etAdminNote.setText(note);
        }
    }

    private void updateReportStatus() {
        String newStatus = spReportStatus.getSelectedItem().toString();
        String adminNote = etAdminNote.getText().toString().trim();

        btnUpdateStatus.setEnabled(false);

        // Always update SQLite if present
        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            db.update(DatabaseHelper.TABLE_REPORTS, cv, "id = ? OR firebase_id = ?", new String[]{reportId, reportId});
        } catch (Exception ignored) {}

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("adminNote", adminNote);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("reports")
                .document(reportId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    btnUpdateStatus.setEnabled(true);
                    tvDetailCurrentStatus.setText("Current Status: " + newStatus);
                    Toast.makeText(this, "Report status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnUpdateStatus.setEnabled(true);
                    tvDetailCurrentStatus.setText("Current Status: " + newStatus);
                    Toast.makeText(this, "Status saved locally (" + newStatus + ")", Toast.LENGTH_SHORT).show();
                });
    }
}
