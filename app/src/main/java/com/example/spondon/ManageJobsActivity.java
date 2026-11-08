package com.example.spondon;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageJobsActivity extends AppCompatActivity {

    private EditText etNewJobTitle, etNewJobCompany, etNewJobSalary, etNewJobLocation, etNewJobDeadline, etNewJobDesc, etNewJobReqs;
    private Spinner spNewJobCategory, spNewJobType;
    private Button btnPublishJob, btnBackManageJobs;

    private TextView tvAdminAppsHeader;
    private ProgressBar progressAdminApps;
    private LinearLayout layoutAdminAppsContainer;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ListenerRegistration appsListener;

    private final String[] categories = {"IT & Tech", "Municipal & Admin", "Trades & Tech", "Healthcare", "Education", "Hospitality"};
    private final String[] jobTypes = {"Full-Time", "Part-Time", "Internship", "Civic Volunteering"};

    private final List<AdminCandidateApp> candidateList = new ArrayList<>();

    private static final String TAG = "ManageJobs";
    /** How long we wait for the server to confirm a publish before handing control back. */
    private static final long PUBLISH_TIMEOUT_MS = 20_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable publishWatchdog;
    private int publishSeq = 0;
    private boolean publishing = false;
    /**
     * Document that the current posting is written to. It is kept until the server confirms the
     * write, so tapping Publish again after a timeout re-writes the SAME document instead of
     * creating a duplicate posting.
     */
    private DocumentReference pendingJobRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_jobs);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etNewJobTitle = findViewById(R.id.etNewJobTitle);
        etNewJobCompany = findViewById(R.id.etNewJobCompany);
        etNewJobSalary = findViewById(R.id.etNewJobSalary);
        etNewJobLocation = findViewById(R.id.etNewJobLocation);
        etNewJobDeadline = findViewById(R.id.etNewJobDeadline);
        etNewJobDesc = findViewById(R.id.etNewJobDesc);
        etNewJobReqs = findViewById(R.id.etNewJobReqs);

        spNewJobCategory = findViewById(R.id.spNewJobCategory);
        spNewJobType = findViewById(R.id.spNewJobType);

        btnPublishJob = findViewById(R.id.btnPublishJob);
        btnBackManageJobs = findViewById(R.id.btnBackManageJobs);

        tvAdminAppsHeader = findViewById(R.id.tvAdminAppsHeader);
        progressAdminApps = findViewById(R.id.progressAdminApps);
        layoutAdminAppsContainer = findViewById(R.id.layoutAdminAppsContainer);

        setupSpinners();

        btnBackManageJobs.setOnClickListener(v -> finish());
        btnPublishJob.setOnClickListener(v -> publishNewJob());

        listenToCandidateApplications();
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNewJobCategory.setAdapter(catAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, jobTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNewJobType.setAdapter(typeAdapter);
    }

    private void setPublishing(boolean value) {
        publishing = value;
        btnPublishJob.setEnabled(!value);
        btnPublishJob.setText(value ? "Publishing..." : "Publish Job Posting");
    }

    private void publishNewJob() {
        if (publishing) return;

        String title = etNewJobTitle.getText().toString().trim();
        String company = etNewJobCompany.getText().toString().trim();
        String salary = etNewJobSalary.getText().toString().trim();
        String location = etNewJobLocation.getText().toString().trim();
        String deadline = etNewJobDeadline.getText().toString().trim();
        String desc = etNewJobDesc.getText().toString().trim();
        String reqs = etNewJobReqs.getText().toString().trim();

        if (title.isEmpty() || company.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please enter title, company and location.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Firestore queues writes while offline and only reports back once the server has them,
        // which used to leave the button disabled forever with no message. Stop early instead.
        if (!FirestoreUi.isOnline(this)) {
            Toast.makeText(this, "No internet connection. Connect to the internet and tap Publish again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Field names/types are exactly what JobsActivity reads (all text, plus createdAt).
        Map<String, Object> job = new HashMap<>();
        job.put("title", title);
        job.put("company", company);
        job.put("category", String.valueOf(spNewJobCategory.getSelectedItem()));
        job.put("jobType", String.valueOf(spNewJobType.getSelectedItem()));
        job.put("salary", salary.isEmpty() ? "Negotiable" : salary);
        job.put("location", location);
        job.put("deadline", deadline.isEmpty() ? "Rolling" : deadline);
        job.put("description", desc);
        job.put("requirements", reqs);
        job.put("createdAt", FieldValue.serverTimestamp());
        job.put("status", "Open");
        job.put("postedBy", user.getUid());

        if (pendingJobRef == null) {
            pendingJobRef = firestore.collection("jobs").document();
        }
        final DocumentReference ref = pendingJobRef;
        final int requestId = ++publishSeq;

        setPublishing(true);

        publishWatchdog = () -> {
            if (requestId != publishSeq || !publishing) return;
            setPublishing(false);
            Toast.makeText(this, "The server has not confirmed the posting yet. Check your connection "
                    + "and tap Publish again - the same posting will be reused, not duplicated.",
                    Toast.LENGTH_LONG).show();
        };
        handler.postDelayed(publishWatchdog, PUBLISH_TIMEOUT_MS);

        // set() on a pre-generated id: the Task completes only after the server has committed it.
        ref.set(job)
                .addOnSuccessListener(unused -> {
                    handler.removeCallbacks(publishWatchdog);
                    pendingJobRef = null;
                    setPublishing(false);
                    Toast.makeText(this, "Job posting published successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Publish failed", e);
                    if (requestId != publishSeq) return;
                    handler.removeCallbacks(publishWatchdog);
                    setPublishing(false);
                    Toast.makeText(this, "Failed to publish job: " + FirestoreUi.describe(e),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void clearForm() {
        etNewJobTitle.setText("");
        etNewJobCompany.setText("");
        etNewJobSalary.setText("");
        etNewJobLocation.setText("");
        etNewJobDeadline.setText("");
        etNewJobDesc.setText("");
        etNewJobReqs.setText("");
    }

    private void listenToCandidateApplications() {
        progressAdminApps.setVisibility(View.VISIBLE);

        // No orderBy("appliedAt"): it drops every application that lacks the field. Sorted below.
        appsListener = firestore.collection("job_applications")
                .addSnapshotListener((value, error) -> {
                    progressAdminApps.setVisibility(View.GONE);

                    if (error != null || value == null) {
                        Log.w(TAG, "job_applications listener failed", error);
                        showApplicationsError(FirestoreUi.describe(error));
                        return;
                    }

                    final List<AdminCandidateApp> loaded = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            AdminCandidateApp app = new AdminCandidateApp();
                            app.id = doc.getId();
                            app.jobTitle = FirestoreUi.str(doc, "jobTitle", "Job");
                            app.company = FirestoreUi.str(doc, "company", "-");
                            app.name = FirestoreUi.str(doc, "applicantName", "Unknown applicant");
                            app.email = FirestoreUi.str(doc, "applicantEmail", "no email");
                            app.phone = FirestoreUi.str(doc, "applicantPhone", null);
                            app.education = FirestoreUi.str(doc, "education", "-");
                            app.skills = FirestoreUi.str(doc, "skills", "-");
                            app.coverNote = FirestoreUi.str(doc, "coverNote", null);
                            app.status = FirestoreUi.str(doc, "status", "Submitted");
                            app.appliedAt = FirestoreUi.millis(doc, "appliedAt");
                            loaded.add(app);
                        } catch (Exception e) {
                            Log.w(TAG, "Skipped unreadable application " + doc.getId(), e);
                        }
                    }

                    Collections.sort(loaded, (a, b) -> Long.compare(b.appliedAt, a.appliedAt));

                    candidateList.clear();
                    candidateList.addAll(loaded);
                    displayCandidateApplications();
                });
    }

    private void showApplicationsError(String reason) {
        candidateList.clear();
        layoutAdminAppsContainer.removeAllViews();
        tvAdminAppsHeader.setText("CANDIDATE APPLICATIONS");

        TextView err = new TextView(this);
        err.setText("Could not load applications: " + reason);
        err.setTextColor(Color.parseColor("#B91C1C"));
        err.setPadding(0, dp(10), 0, 0);
        layoutAdminAppsContainer.addView(err);
    }

    private void displayCandidateApplications() {
        layoutAdminAppsContainer.removeAllViews();
        tvAdminAppsHeader.setText("CANDIDATE APPLICATIONS (" + candidateList.size() + ")");

        if (candidateList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No candidate applications submitted yet.");
            empty.setTextColor(Color.parseColor("#64748B"));
            empty.setPadding(0, dp(10), 0, 0);
            layoutAdminAppsContainer.addView(empty);
            return;
        }

        for (AdminCandidateApp app : candidateList) {
            addCandidateCard(app);
        }
    }

    private void addCandidateCard(AdminCandidateApp app) {
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

        TextView title = new TextView(this);
        title.setText("Position: " + app.jobTitle + " (" + app.company + ")");
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView candidate = new TextView(this);
        candidate.setText("Applicant: " + app.name + "\nPhone: " + (app.phone != null ? app.phone : "not provided") + " • " + app.email
                + "\nEducation: " + app.education + "\nSkills: " + app.skills);
        candidate.setTextSize(13);
        candidate.setTextColor(Color.parseColor("#475569"));
        candidate.setPadding(0, dp(4), 0, dp(4));
        card.addView(candidate);

        if (app.coverNote != null && !app.coverNote.isEmpty()) {
            TextView note = new TextView(this);
            note.setText("Note: \"" + app.coverNote + "\"");
            note.setTextSize(12);
            note.setTextColor(Color.parseColor("#64748B"));
            card.addView(note);
        }

        TextView status = new TextView(this);
        status.setText("Status: " + app.status.toUpperCase());
        status.setTextSize(13);
        status.setTypeface(null, Typeface.BOLD);
        status.setTextColor(Color.parseColor("#4338CA"));
        status.setPadding(0, dp(4), 0, dp(8));
        card.addView(status);

        // Action Buttons
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button btnCall = new Button(this);
        btnCall.setText("Call Candidate");
        btnCall.setTextSize(12);
        btnCall.setOnClickListener(v -> {
            if (app.phone != null && !app.phone.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + app.phone)));
            }
        });

        Button btnStatus = new Button(this);
        btnStatus.setText("Change Status");
        btnStatus.setTextSize(12);
        btnStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4338CA")));
        btnStatus.setTextColor(Color.WHITE);
        btnStatus.setOnClickListener(v -> showStatusDialog(app));

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnCall.setLayoutParams(btnParams);
        btnStatus.setLayoutParams(btnParams);

        actions.addView(btnCall);
        actions.addView(btnStatus);
        card.addView(actions);

        layoutAdminAppsContainer.addView(card);
    }

    private void showStatusDialog(AdminCandidateApp app) {
        String[] statuses = {"Under Review", "Shortlisted", "Rejected"};

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Update Application Status")
                .setView(spinner)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Update", (d, which) -> {
                    String newStatus = spinner.getSelectedItem().toString();
                    firestore.collection("job_applications").document(app.id)
                            .update("status", newStatus)
                            .addOnSuccessListener(unused -> Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Could not update status: " + FirestoreUi.describe(e), Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (appsListener != null) {
            appsListener.remove();
        }
        super.onDestroy();
    }

    private static class AdminCandidateApp {
        String id, jobTitle, company, name, email, phone, education, skills, coverNote, status;
        long appliedAt = 0L;
    }
}
