
package com.example.spondon;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProjectUpdatesActivity extends AppCompatActivity {

    private TextView tvUpdateProjectTitle;
    private TextView tvNoUpdates;
    private ProgressBar updatesLoading;
    private LinearLayout layoutProjectUpdates;

    private FirebaseFirestore firestore;
    private ListenerRegistration updatesListener;

    private String projectId;
    private String projectTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_updates);

        tvUpdateProjectTitle = findViewById(R.id.tvUpdateProjectTitle);
        tvNoUpdates = findViewById(R.id.tvNoUpdates);
        updatesLoading = findViewById(R.id.updatesLoading);
        layoutProjectUpdates = findViewById(R.id.layoutProjectUpdates);

        projectId = getIntent().getStringExtra("projectId");
        projectTitle = getIntent().getStringExtra("projectTitle");

        if (projectId == null || projectId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Project information missing.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        if (projectTitle == null) {
            projectTitle = "Project Updates";
        }

        tvUpdateProjectTitle.setText(projectTitle);

        firestore = FirebaseFirestore.getInstance();

        loadProjectUpdates();
    }

    private void loadProjectUpdates() {

        updatesLoading.setVisibility(View.VISIBLE);
        tvNoUpdates.setVisibility(View.GONE);

        updatesListener = firestore.collection("projects")
                .document(projectId)
                .collection("updates")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    updatesLoading.setVisibility(View.GONE);

                    if (error != null) {

                        tvNoUpdates.setText(
                                "Unable to load project updates."
                        );

                        tvNoUpdates.setVisibility(View.VISIBLE);

                        Toast.makeText(
                                this,
                                "Update loading failed: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    layoutProjectUpdates.removeAllViews();

                    if (value == null || value.isEmpty()) {

                        tvNoUpdates.setText(
                                "No project updates available yet."
                        );

                        tvNoUpdates.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvNoUpdates.setVisibility(View.GONE);

                    for (DocumentSnapshot document : value.getDocuments()) {

                        String title = document.getString("title");
                        String description =
                                document.getString("description");

                        String status = document.getString("status");

                        Object dateValue = document.get("createdAt");

                        if (title == null) {
                            title = "Project Update";
                        }

                        if (description == null) {
                            description = "No description available.";
                        }

                        if (status == null) {
                            status = "Update";
                        }

                        String date = formatDate(dateValue);

                        addUpdateCard(title, description, status, date);
                    }
                });
    }

    private void addUpdateCard(
            String title,
            String description,
            String status,
            String date
    ) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(16), dpToPx(16),
                dpToPx(16), dpToPx(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dpToPx(14));
        background.setStroke(
                dpToPx(1),
                Color.rgb(225, 230, 238)
        );

        card.setBackground(background);
        card.setElevation(dpToPx(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dpToPx(14));
        card.setLayoutParams(cardParams);

        TextView titleView = createText(
                title,
                18,
                "#172B4D",
                true
        );

        TextView statusView = createText(
                "Status: " + status,
                13,
                getStatusColor(status),
                true
        );

        TextView descriptionView = createText(
                description,
                15,
                "#475467",
                false
        );

        TextView dateView = createText(
                "Date: " + date,
                12,
                "#667085",
                false
        );

        card.addView(titleView);
        card.addView(statusView);
        card.addView(descriptionView);
        card.addView(dateView);

        layoutProjectUpdates.addView(card);
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
        textView.setPadding(0, dpToPx(5), 0, dpToPx(5));

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

    private String formatDate(Object dateValue) {

        if (dateValue instanceof Timestamp) {

            Date date = ((Timestamp) dateValue).toDate();

            return new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            ).format(date);
        }

        if (dateValue instanceof Date) {

            return new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            ).format((Date) dateValue);
        }

        if (dateValue instanceof String) {
            return (String) dateValue;
        }

        return "Date not specified";
    }

    private int dpToPx(int dp) {

        return (int) (dp * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {

        if (updatesListener != null) {
            updatesListener.remove();
        }

        super.onDestroy();
    }
}
