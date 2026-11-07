
package com.example.spondon;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReportDetailsActivity extends AppCompatActivity {

    TextView tvId, tvCategory, tvTitle;
    TextView tvDescription, tvLocation, tvStatus, tvDate;
    TextView tvAdminNote;

    ImageView ivDetailReportImage;
    Button btnBack;

    DatabaseHelper databaseHelper;
    FirebaseFirestore firestore;

    String firebaseId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_details);

        tvId = findViewById(R.id.tvDetailReportId);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvDate = findViewById(R.id.tvDetailDate);
        tvAdminNote = findViewById(R.id.tvDetailAdminNote);

        ivDetailReportImage = findViewById(R.id.ivDetailReportImage);
        btnBack = findViewById(R.id.btnDetailBack);

        databaseHelper = new DatabaseHelper(this);
        firestore = FirebaseFirestore.getInstance();

        int reportId = getIntent().getIntExtra("REPORT_ID", -1);

        btnBack.setOnClickListener(v -> finish());

        if (reportId == -1) {
            Toast.makeText(
                    this,
                    "Invalid Report ID!",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        loadReportDetails(reportId);
    }

    private void loadReportDetails(int reportId) {

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {

            cursor = db.rawQuery(
                    "SELECT * FROM reports WHERE id = ?",
                    new String[]{String.valueOf(reportId)}
            );

            if (cursor.moveToFirst()) {

                tvId.setText("Report ID: " + reportId);

                tvCategory.setText(
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("category")
                        )
                );

                tvTitle.setText(
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("title")
                        )
                );

                tvDescription.setText(
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("description")
                        )
                );

                tvLocation.setText(
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("location")
                        )
                );

                tvStatus.setText(
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("status")
                        )
                );

                tvDate.setText(
                        "Submitted: " +
                                cursor.getString(
                                        cursor.getColumnIndexOrThrow("created_at")
                                )
                );

                firebaseId = cursor.getString(
                        cursor.getColumnIndexOrThrow("firebase_id")
                );

                String imageUri = cursor.getString(
                        cursor.getColumnIndexOrThrow("image_uri")
                );

                if (imageUri != null && !imageUri.isEmpty()) {

                    try {
                        ivDetailReportImage.setImageURI(Uri.parse(imageUri));
                        ivDetailReportImage.setVisibility(ImageView.VISIBLE);

                    } catch (Exception e) {
                        ivDetailReportImage.setVisibility(ImageView.GONE);
                    }

                } else {
                    ivDetailReportImage.setVisibility(ImageView.GONE);
                }

            } else {

                Toast.makeText(
                        this,
                        "Report not found!",
                        Toast.LENGTH_LONG
                ).show();

                finish();
                return;
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error loading report: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            db.close();
        }

        syncAdminUpdate();
    }

    private void syncAdminUpdate() {

        if (firebaseId == null || firebaseId.isEmpty()) {
            tvAdminNote.setText("No online admin update available.");
            return;
        }

        firestore.collection("reports")
                .document(firebaseId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        return;
                    }

                    String status = document.getString("status");
                    String adminNote = document.getString("adminNote");

                    if (status != null) {
                        tvStatus.setText(status);

                        SQLiteDatabase db =
                                databaseHelper.getWritableDatabase();

                        android.content.ContentValues values =
                                new android.content.ContentValues();

                        values.put("status", status);

                        db.update(
                                DatabaseHelper.TABLE_REPORTS,
                                values,
                                "firebase_id = ?",
                                new String[]{firebaseId}
                        );

                        db.close();
                    }

                    if (adminNote != null && !adminNote.trim().isEmpty()) {
                        tvAdminNote.setText(adminNote);
                    } else {
                        tvAdminNote.setText(
                                "No update from administration yet."
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to fetch admin update.",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
