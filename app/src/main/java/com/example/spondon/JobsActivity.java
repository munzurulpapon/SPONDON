package com.example.spondon;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JobsActivity extends AppCompatActivity {

    private static final String TAG = "JobsActivity";

    private EditText etSearchJobs;
    private TextView tvJobsCount, tvNoJobs, tvMyApplicationsLink;
    private ProgressBar progressJobs;
    private LinearLayout layoutJobsContainer;
    private Button btnBackJobs;

    private Button btnJobAll, btnJobIT, btnJobAdmin, btnJobTrades, btnJobPartTime;

    private FirebaseFirestore firestore;
    private ListenerRegistration jobsListener;

    private final List<JobItem> allJobs = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs);

        firestore = FirebaseFirestore.getInstance();

        etSearchJobs = findViewById(R.id.etSearchJobs);
        tvJobsCount = findViewById(R.id.tvJobsCount);
        tvNoJobs = findViewById(R.id.tvNoJobs);
        tvMyApplicationsLink = findViewById(R.id.tvMyApplicationsLink);
        progressJobs = findViewById(R.id.progressJobs);
        layoutJobsContainer = findViewById(R.id.layoutJobsContainer);
        btnBackJobs = findViewById(R.id.btnBackJobs);

        btnJobAll = findViewById(R.id.btnJobAll);
        btnJobIT = findViewById(R.id.btnJobIT);
        btnJobAdmin = findViewById(R.id.btnJobAdmin);
        btnJobTrades = findViewById(R.id.btnJobTrades);
        btnJobPartTime = findViewById(R.id.btnJobPartTime);

        btnBackJobs.setOnClickListener(v -> finish());
        tvMyApplicationsLink.setOnClickListener(v -> {
            startActivity(new Intent(JobsActivity.this, MyJobApplicationsActivity.class));
        });

        setupFilters();
        setupSearch();
        loadSeedJobs();
        listenToFirestoreJobs();
    }

    private void setupFilters() {
        btnJobAll.setOnClickListener(v -> setCategory("All"));
        btnJobIT.setOnClickListener(v -> setCategory("IT & Tech"));
        btnJobAdmin.setOnClickListener(v -> setCategory("Municipal & Admin"));
        btnJobTrades.setOnClickListener(v -> setCategory("Trades & Tech"));
        btnJobPartTime.setOnClickListener(v -> setCategory("Part-Time"));
    }

    private void setCategory(String category) {
        selectedCategory = category;
        displayJobs();
    }

    private void setupSearch() {
        etSearchJobs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayJobs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSeedJobs() {
        allJobs.clear();

        allJobs.add(new JobItem("job_1", "Junior Android Developer", "Khulna Digital Solutions", "IT & Tech",
                "Full-Time", "৳28,000 - ৳35,000 / month", "Sonadanga, Khulna", "2026-12-15",
                "Proficiency in Java, Android SDK, SQLite and REST APIs. Experience building user-friendly mobile UIs.",
                "Build and maintain smart city Android citizen client applications, test local database sync, optimize memory."));

        allJobs.add(new JobItem("job_2", "Smart Infrastructure IoT Technician", "Khulna Municipal Services", "Trades & Tech",
                "Full-Time", "৳22,000 - ৳26,000 / month", "Boyra, Khulna", "2026-11-30",
                "Diploma in Electrical or Electronics. Experience with smart streetlights, sensors and solar meters.",
                "Install, test and maintain automated streetlight sensors, flood monitoring gauges and municipal solar units."));

        allJobs.add(new JobItem("job_3", "GIS & Spatial Data Field Enumerator", "Urban Planning Directorate", "IT & Tech",
                "Part-Time", "৳15,000 / month (Stipend)", "Khulna City Sadar", "2026-11-25",
                "Undergraduate in Urban Planning, Geography or CSE. Basic familiarity with OpenStreetMap / GPS mapping.",
                "Conduct ground-truth verification of city roads, public facilities and civic drainage hotspots using mobile GPS tools."));

        allJobs.add(new JobItem("job_4", "Civic Helpdesk & Citizen Service Assistant", "Khulna City Citizen Care", "Municipal & Admin",
                "Full-Time", "৳18,000 - ৳22,000 / month", "KDA Avenue, Khulna", "2026-12-05",
                "HSC / Bachelor's in any discipline. Courteous communication skills in Bangla and English.",
                "Log citizen problem reports, coordinate dispatch with municipal field engineers, verify resolution status."));

        allJobs.add(new JobItem("job_5", "Solar Inverter & Panel Maintenance Tech", "Green Energy South-Bengal", "Trades & Tech",
                "Full-Time", "৳25,000 - ৳30,000 / month", "Khalishpur Industrial Zone", "2026-12-10",
                "Trade certificate in Electrical wiring. Valid motorcycle driving license preferred.",
                "Routine inspection of rooftop solar arrays, battery backups, inverter circuit diagnostics and safety testing."));

        allJobs.add(new JobItem("job_6", "Digital Literacy Part-Time Instructor", "Youth Empowerment Center", "Part-Time",
                "Part-Time", "৳12,000 / month", "Gollamari, Khulna", "2026-11-28",
                "Strong grasp of basic computers, office productivity, digital citizen services and cyber awareness.",
                "Conduct weekend batches for youth and senior citizens on using e-government services and basic computing."));
    }

    private void listenToFirestoreJobs() {
        progressJobs.setVisibility(View.VISIBLE);

        jobsListener = firestore.collection("jobs")
                .addSnapshotListener((value, error) -> {
                    progressJobs.setVisibility(View.GONE);

                    if (error != null || value == null) {
                        // Used to be swallowed silently, which made a rules/permission problem look
                        // like "the admin's job never showed up". Tell the user, keep the seed jobs.
                        Log.w(TAG, "jobs listener failed", error);
                        Toast.makeText(this, "Could not load published jobs: "
                                + FirestoreUi.describe(error), Toast.LENGTH_LONG).show();
                        displayJobs();
                        return;
                    }

                    loadSeedJobs();

                    final List<JobItem> published = new ArrayList<>();
                    final Map<String, Long> created = new HashMap<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            String title = FirestoreUi.str(doc, "title");
                            if (title == null) continue;   // not a usable posting

                            // Blank text typed by the admin (e.g. an empty description) falls back
                            // to a readable default instead of showing an empty field.
                            published.add(new JobItem(
                                    doc.getId(),
                                    title,
                                    FirestoreUi.str(doc, "company", "Smart City Employer"),
                                    FirestoreUi.str(doc, "category", "General"),
                                    FirestoreUi.str(doc, "jobType", "Full-Time"),
                                    FirestoreUi.str(doc, "salary", "Competitive"),
                                    FirestoreUi.str(doc, "location", "Khulna"),
                                    FirestoreUi.str(doc, "deadline", "Rolling"),
                                    FirestoreUi.str(doc, "requirements", "Standard qualifications required."),
                                    FirestoreUi.str(doc, "description", "Detailed duties available on application.")
                            ));
                            created.put(doc.getId(), FirestoreUi.millis(doc, "createdAt"));
                        } catch (Exception e) {
                            Log.w(TAG, "Skipped unreadable job " + doc.getId(), e);
                        }
                    }

                    // Newest published job first, ahead of the built-in sample jobs.
                    Collections.sort(published, (a, b) -> Long.compare(
                            created.containsKey(b.id) ? created.get(b.id) : 0L,
                            created.containsKey(a.id) ? created.get(a.id) : 0L));
                    allJobs.addAll(0, published);

                    displayJobs();
                });
    }

    private void displayJobs() {
        layoutJobsContainer.removeAllViews();
        int count = 0;

        for (JobItem job : allJobs) {
            boolean matchesCat = "All".equalsIgnoreCase(selectedCategory)
                    || job.category.equalsIgnoreCase(selectedCategory)
                    || job.jobType.equalsIgnoreCase(selectedCategory);

            boolean matchesSearch = job.title.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || job.company.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || job.description.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || job.requirements.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCat && matchesSearch) {
                addJobCard(job);
                count++;
            }
        }

        tvJobsCount.setText("Active Opportunities (" + count + ")");
        tvNoJobs.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void addJobCard(JobItem job) {
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

        // Header: Category & Type
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView catTag = new TextView(this);
        catTag.setText(job.category.toUpperCase(Locale.ROOT));
        catTag.setTextColor(Color.parseColor("#4338CA"));
        catTag.setTextSize(11);
        catTag.setTypeface(null, Typeface.BOLD);
        catTag.setBackgroundColor(Color.parseColor("#EEF2FF"));
        catTag.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        catTag.setLayoutParams(catParams);

        TextView typeTag = new TextView(this);
        typeTag.setText(job.jobType.toUpperCase(Locale.ROOT));
        typeTag.setTextColor(Color.parseColor("#047857"));
        typeTag.setTextSize(11);
        typeTag.setTypeface(null, Typeface.BOLD);
        typeTag.setBackgroundColor(Color.parseColor("#D1FAE5"));
        typeTag.setPadding(dp(8), dp(3), dp(8), dp(3));

        top.addView(catTag);
        top.addView(typeTag);
        card.addView(top);

        // Title & Company
        TextView title = new TextView(this);
        title.setText(job.title);
        title.setTextSize(17);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(6), 0, dp(2));
        card.addView(title);

        TextView company = new TextView(this);
        company.setText(job.company + " • " + job.location);
        company.setTextSize(13);
        company.setTextColor(Color.parseColor("#475569"));
        card.addView(company);

        // Salary & Deadline
        TextView salary = new TextView(this);
        salary.setText(job.salary + " • Deadline: " + job.deadline);
        salary.setTextSize(13);
        salary.setTextColor(Color.parseColor("#15803D"));
        salary.setTypeface(null, Typeface.BOLD);
        salary.setPadding(0, dp(2), 0, dp(8));
        card.addView(salary);

        // Action Buttons Row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button btnDetails = new Button(this);
        btnDetails.setText("Details");
        btnDetails.setTextSize(12);
        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(JobsActivity.this, JobDetailsActivity.class);
            intent.putExtra("JOB_ID", job.id);
            intent.putExtra("JOB_TITLE", job.title);
            intent.putExtra("JOB_COMPANY", job.company);
            intent.putExtra("JOB_CATEGORY", job.category);
            intent.putExtra("JOB_TYPE", job.jobType);
            intent.putExtra("JOB_SALARY", job.salary);
            intent.putExtra("JOB_LOCATION", job.location);
            intent.putExtra("JOB_DEADLINE", job.deadline);
            intent.putExtra("JOB_REQS", job.requirements);
            intent.putExtra("JOB_DESC", job.description);
            startActivity(intent);
        });

        Button btnApply = new Button(this);
        btnApply.setText("Apply Now");
        btnApply.setTextSize(12);
        btnApply.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4338CA")));
        btnApply.setTextColor(Color.WHITE);
        btnApply.setOnClickListener(v -> {
            Intent intent = new Intent(JobsActivity.this, ApplyJobActivity.class);
            intent.putExtra("JOB_ID", job.id);
            intent.putExtra("JOB_TITLE", job.title);
            intent.putExtra("JOB_COMPANY", job.company);
            startActivity(intent);
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnDetails.setLayoutParams(btnParams);
        btnApply.setLayoutParams(btnParams);

        actions.addView(btnDetails);
        actions.addView(btnApply);
        card.addView(actions);

        layoutJobsContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (jobsListener != null) {
            jobsListener.remove();
        }
        super.onDestroy();
    }

    public static class JobItem {
        public String id, title, company, category, jobType, salary, location, deadline, requirements, description;

        public JobItem(String id, String title, String company, String category,
                       String jobType, String salary, String location, String deadline,
                       String requirements, String description) {
            this.id = id;
            this.title = title;
            this.company = company;
            this.category = category;
            this.jobType = jobType;
            this.salary = salary;
            this.location = location;
            this.deadline = deadline;
            this.requirements = requirements;
            this.description = description;
        }
    }
}
