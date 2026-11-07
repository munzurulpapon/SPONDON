
package com.example.spondon;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PublicProjectsActivity extends AppCompatActivity {

    private EditText etProjectSearch;
    private TextView tvProjectCount, tvNoProjects;
    private ProgressBar projectLoading;
    private LinearLayout layoutProjects;

    private FirebaseFirestore firestore;
    private ListenerRegistration projectsListener;

    private final List<Project> allProjects = new ArrayList<>();

    private String selectedStatus = "All";
    private String searchText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_projects);

        initializeViews();

        firestore = FirebaseFirestore.getInstance();

        setupSearch();
        setupFilters();

        loadProjects();
    }

    private void initializeViews() {

        etProjectSearch = findViewById(R.id.etProjectSearch);
        tvProjectCount = findViewById(R.id.tvProjectCount);
        tvNoProjects = findViewById(R.id.tvNoProjects);
        projectLoading = findViewById(R.id.projectLoading);
        layoutProjects = findViewById(R.id.layoutProjects);
    }

    private void setupSearch() {

        etProjectSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                searchText = s.toString().trim().toLowerCase(Locale.ROOT);
                displayProjects();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilters() {

        Button btnAll = findViewById(R.id.btnAllProjects);
        Button btnOngoing = findViewById(R.id.btnOngoingProjects);
        Button btnDelayed = findViewById(R.id.btnDelayedProjects);
        Button btnCompleted = findViewById(R.id.btnCompletedProjects);

        btnAll.setOnClickListener(v -> changeFilter("All"));
        btnOngoing.setOnClickListener(v -> changeFilter("Ongoing"));
        btnDelayed.setOnClickListener(v -> changeFilter("Delayed"));
        btnCompleted.setOnClickListener(v -> changeFilter("Completed"));
    }

    private void changeFilter(String status) {

        selectedStatus = status;
        displayProjects();
    }

    private void loadProjects() {

        projectLoading.setVisibility(View.VISIBLE);
        tvNoProjects.setVisibility(View.GONE);

        projectsListener = firestore.collection("projects")
                .addSnapshotListener((value, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    projectLoading.setVisibility(View.GONE);

                    if (error != null) {

                        Toast.makeText(
                                this,
                                "Failed to load projects: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        tvNoProjects.setText("Unable to load projects.");
                        tvNoProjects.setVisibility(View.VISIBLE);

                        return;
                    }

                    allProjects.clear();

                    if (value != null) {

                        for (DocumentSnapshot document : value.getDocuments()) {

                            Project project = new Project();

                            project.id = document.getId();

                            project.title = document.getString("title");
                            project.description = document.getString("description");
                            project.location = document.getString("location");
                            project.status = document.getString("status");
                            project.agency = document.getString("agency");
                            project.startDate = document.getString("startDate");
                            project.endDate = document.getString("endDate");

                            Object budgetValue = document.get("budget");

                            if (budgetValue instanceof Number) {
                                project.budget =
                                        ((Number) budgetValue).doubleValue();
                            }

                            Object progressValue = document.get("progress");

                            if (progressValue instanceof Number) {
                                project.progress =
                                        ((Number) progressValue).intValue();
                            }

                            if (project.title == null) {
                                project.title = "Untitled Project";
                            }

                            if (project.description == null) {
                                project.description = "No description available.";
                            }

                            if (project.location == null) {
                                project.location = "Location not specified";
                            }

                            if (project.status == null) {
                                project.status = "Planned";
                            }

                            if (project.agency == null) {
                                project.agency = "Not specified";
                            }

                            project.progress = Math.max(
                                    0, Math.min(100, project.progress)
                            );

                            allProjects.add(project);
                        }
                    }

                    displayProjects();
                });
    }

    private void displayProjects() {

        layoutProjects.removeAllViews();

        int visibleCount = 0;

        for (Project project : allProjects) {

            boolean matchesStatus =
                    selectedStatus.equals("All") ||
                            project.status.equalsIgnoreCase(selectedStatus);

            String searchableText =
                    (project.title + " " +
                            project.location + " " +
                            project.agency).toLowerCase(Locale.ROOT);

            boolean matchesSearch = searchableText.contains(searchText);

            if (matchesStatus && matchesSearch) {

                addProjectCard(project);
                visibleCount++;
            }
        }

        tvProjectCount.setText(
                "City Development Projects (" + visibleCount + ")"
        );

        if (visibleCount == 0) {

            tvNoProjects.setText(
                    allProjects.isEmpty()
                            ? "No projects available yet."
                            : "No matching projects found."
            );

            tvNoProjects.setVisibility(View.VISIBLE);

        } else {

            tvNoProjects.setVisibility(View.GONE);
        }
    }

    private void addProjectCard(Project project) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 18, 18, 18);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(18);
        background.setStroke(1, Color.rgb(225, 230, 238));

        card.setBackground(background);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 14);

        card.setLayoutParams(cardParams);
        card.setElevation(3);

        TextView title = createText(
                project.title, 18, "#172B4D", true
        );

        TextView location = createText(
                "Location: " + project.location,
                14, "#667085", false
        );

        TextView agency = createText(
                "Agency: " + project.agency,
                14, "#667085", false
        );

        TextView status = createText(
                "Status: " + project.status,
                14, getStatusColor(project.status), true
        );

        TextView budget = createText(
                "Budget: " + formatBudget(project.budget),
                14, "#344054", true
        );

        TextView progressText = createText(
                "Project Progress: " + project.progress + "%",
                14, "#172B4D", true
        );

        ProgressBar progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        progressBar.setMax(100);
        progressBar.setProgress(project.progress);

        progressBar.setProgressTintList(
                ColorStateList.valueOf(
                        Color.parseColor(getStatusColor(project.status))
                )
        );

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(10)
                );

        progressParams.setMargins(0, 8, 0, 12);

        progressBar.setLayoutParams(progressParams);

        TextView timeline = createText(
                "Timeline: " + getTimelineText(project),
                13, "#667085", false
        );

        Button detailsButton = new Button(this);
        detailsButton.setText("View Project Details");

        detailsButton.setOnClickListener(
                v -> showProjectDetails(project)
        );

        Button updatesButton = new Button(this);
        updatesButton.setText("View Project Updates");

        updatesButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PublicProjectsActivity.this,
                    ProjectUpdatesActivity.class
            );

            intent.putExtra("projectId", project.id);
            intent.putExtra("projectTitle", project.title);

            startActivity(intent);
        });

        card.addView(title);
        card.addView(location);
        card.addView(agency);
        card.addView(status);
        card.addView(budget);
        card.addView(progressText);
        card.addView(progressBar);
        card.addView(timeline);
        card.addView(detailsButton);
        card.addView(updatesButton);

        layoutProjects.addView(card);
    }

    private TextView createText(
            String value,
            int size,
            String color,
            boolean bold
    ) {

        TextView textView = new TextView(this);

        textView.setText(value);
        textView.setTextSize(size);
        textView.setTextColor(Color.parseColor(color));
        textView.setPadding(0, 5, 0, 5);

        if (bold) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        return textView;
    }

    private String getStatusColor(String status) {

        if (status.equalsIgnoreCase("Completed")) {
            return "#16803C";
        }

        if (status.equalsIgnoreCase("Delayed")) {
            return "#D92D20";
        }

        if (status.equalsIgnoreCase("Ongoing")) {
            return "#175CD3";
        }

        return "#667085";
    }

    private String formatBudget(double budget) {

        if (budget <= 0) {
            return "Not specified";
        }

        NumberFormat formatter =
                NumberFormat.getNumberInstance(Locale.US);

        return "\u09F3" + formatter.format(budget);
    }

    private String getTimelineText(Project project) {

        String start = project.startDate == null
                ? "Not specified" : project.startDate;

        String end = project.endDate == null
                ? "Not specified" : project.endDate;

        return start + " to " + end;
    }

    private String getDurationText(Project project) {

        if (project.startDate == null || project.endDate == null) {
            return "Timeline information unavailable.";
        }

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            format.setLenient(false);

            Date startDate = format.parse(project.startDate);
            Date endDate = format.parse(project.endDate);

            if (startDate == null || endDate == null ||
                    endDate.before(startDate)) {

                return "Invalid project dates.";
            }

            long totalDays = TimeUnit.MILLISECONDS.toDays(
                    endDate.getTime() - startDate.getTime()
            );

            long remainingDays = TimeUnit.MILLISECONDS.toDays(
                    endDate.getTime() - new Date().getTime()
            );

            String result = "Total duration: " + totalDays + " days";

            if (project.status.equalsIgnoreCase("Completed")) {

                result += "\nProject marked as completed.";

            } else if (remainingDays >= 0) {

                result += "\nDays remaining: " + remainingDays;

            } else {

                result += "\nPast scheduled deadline by "
                        + Math.abs(remainingDays) + " days.";
            }

            return result;

        } catch (Exception e) {

            return "Timeline information unavailable.";
        }
    }

    private void showProjectDetails(Project project) {

        String details =
                "Project: " + project.title +

                        "\n\nDescription: " + project.description +

                        "\n\nLocation: " + project.location +

                        "\n\nAgency: " + project.agency +

                        "\n\nBudget: " + formatBudget(project.budget) +

                        "\n\nStatus: " + project.status +

                        "\n\nProgress: " + project.progress + "%" +

                        "\n\nStart Date: " +
                        (project.startDate == null
                                ? "Not specified" : project.startDate) +

                        "\n\nEnd Date: " +
                        (project.endDate == null
                                ? "Not specified" : project.endDate) +

                        "\n\n" + getDurationText(project);

        new AlertDialog.Builder(this)
                .setTitle("Project Details")
                .setMessage(details)
                .setPositiveButton("Close", null)
                .show();
    }

    private int dpToPx(int dp) {

        return (int) (dp * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {

        if (projectsListener != null) {
            projectsListener.remove();
        }

        super.onDestroy();
    }

    private static class Project {

        String id;
        String title;
        String description;
        String location;
        String status;
        String agency;
        String startDate;
        String endDate;

        double budget;
        int progress;
    }
}
