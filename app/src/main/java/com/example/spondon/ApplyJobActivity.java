package com.example.spondon;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ApplyJobActivity extends AppCompatActivity {

    private TextView tvApplyJobHeader;
    private EditText etApplicantName, etApplicantEmail, etApplicantPhone, etApplicantEdu, etApplicantSkills, etApplicantCover;
    private Button btnSubmitJobApp, btnCancelJobApp;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private DatabaseHelper databaseHelper;

    private String jobId = "";
    private String jobTitle = "Job Position";
    private String company = "Employer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_job);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(this);

        tvApplyJobHeader = findViewById(R.id.tvApplyJobHeader);
        etApplicantName = findViewById(R.id.etApplicantName);
        etApplicantEmail = findViewById(R.id.etApplicantEmail);
        etApplicantPhone = findViewById(R.id.etApplicantPhone);
        etApplicantEdu = findViewById(R.id.etApplicantEdu);
        etApplicantSkills = findViewById(R.id.etApplicantSkills);
        etApplicantCover = findViewById(R.id.etApplicantCover);

        btnSubmitJobApp = findViewById(R.id.btnSubmitJobApp);
        btnCancelJobApp = findViewById(R.id.btnCancelJobApp);

        String idExtra = getIntent().getStringExtra("JOB_ID");
        String titleExtra = getIntent().getStringExtra("JOB_TITLE");
        String compExtra = getIntent().getStringExtra("JOB_COMPANY");

        if (idExtra != null) jobId = idExtra;
        if (titleExtra != null) jobTitle = titleExtra;
        if (compExtra != null) company = compExtra;

        tvApplyJobHeader.setText("Applying for: " + jobTitle + " at " + company);

        // Pre-fill profile if logged in
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            etApplicantEmail.setText(auth.getCurrentUser().getEmail());

            firestore.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    if (name != null) etApplicantName.setText(name);
                    if (phone != null) etApplicantPhone.setText(phone);
                }
            });
        }

        btnCancelJobApp.setOnClickListener(v -> finish());
        btnSubmitJobApp.setOnClickListener(v -> submitApplication());
    }

    private void submitApplication() {
        String name = etApplicantName.getText().toString().trim();
        String email = etApplicantEmail.getText().toString().trim();
        String phone = etApplicantPhone.getText().toString().trim();
        String edu = etApplicantEdu.getText().toString().trim();
        String skills = etApplicantSkills.getText().toString().trim();
        String cover = etApplicantCover.getText().toString().trim();

        if (name.isEmpty()) {
            etApplicantName.setError("Enter your full name");
            return;
        }

        if (email.isEmpty()) {
            etApplicantEmail.setError("Enter email address");
            return;
        }

        if (phone.isEmpty()) {
            etApplicantPhone.setError("Enter contact phone");
            return;
        }

        if (edu.isEmpty()) {
            etApplicantEdu.setError("Enter education background");
            return;
        }

        if (skills.isEmpty()) {
            etApplicantSkills.setError("Summarize relevant skills");
            return;
        }

        btnSubmitJobApp.setEnabled(false);

        // 1. Save locally in SQLite
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("job_id", jobId);
        cv.put("job_title", jobTitle);
        cv.put("company", company);
        cv.put("applicant_name", name);
        cv.put("applicant_phone", phone);
        cv.put("applicant_email", email);
        cv.put("qualifications", edu + " • Skills: " + skills);
        cv.put("cover_note", cover);
        cv.put("status", "Submitted");
        db.insert(DatabaseHelper.TABLE_JOB_APPLICATIONS, null, cv);
        db.close();

        // 2. Save in Firestore if logged in
        if (auth.getCurrentUser() != null) {
            Map<String, Object> appMap = new HashMap<>();
            appMap.put("jobId", jobId);
            appMap.put("jobTitle", jobTitle);
            appMap.put("company", company);
            appMap.put("applicantUid", auth.getCurrentUser().getUid());
            appMap.put("applicantName", name);
            appMap.put("applicantEmail", email);
            appMap.put("applicantPhone", phone);
            appMap.put("education", edu);
            appMap.put("skills", skills);
            appMap.put("coverNote", cover);
            appMap.put("status", "Submitted");
            appMap.put("appliedAt", FieldValue.serverTimestamp());

            firestore.collection("job_applications").add(appMap)
                    .addOnSuccessListener(doc -> {
                        btnSubmitJobApp.setEnabled(true);
                        Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmitJobApp.setEnabled(true);
                        Toast.makeText(this, "Application saved locally, but online sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
        } else {
            btnSubmitJobApp.setEnabled(true);
            Toast.makeText(this, "Application recorded locally.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}
