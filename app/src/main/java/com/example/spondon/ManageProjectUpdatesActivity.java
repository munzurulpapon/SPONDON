
package com.example.spondon;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageProjectUpdatesActivity extends AppCompatActivity {

    private Spinner spinnerProjects, spinnerUpdateStatus;
    private EditText etUpdateTitle, etUpdateDescription;
    private Button btnPublishUpdate;
    private ProgressBar progressPublishUpdate;
    private LinearLayout layoutAdminProjectUpdates;
    private TextView tvNoAdminUpdates;

    private FirebaseFirestore firestore;

    private final List<ProjectItem> projectList = new ArrayList<>();

    private ListenerRegistration projectListener;
    private ListenerRegistration updateListener;

    private final String[] statuses = {
            "Planned",
            "Ongoing",
            "Delayed",
            "Completed"
    };

    private String selectedProjectId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_project_updates);

        firestore = FirebaseFirestore.getInstance();

        spinnerProjects = findViewById(R.id.spinnerProjects);
        spinnerUpdateStatus = findViewById(R.id.spinnerUpdateStatus);
        etUpdateTitle = findViewById(R.id.etUpdateTitle);
        etUpdateDescription = findViewById(R.id.etUpdateDescription);
        btnPublishUpdate = findViewById(R.id.btnPublishUpdate);
        progressPublishUpdate = findViewById(R.id.progressPublishUpdate);
        layoutAdminProjectUpdates = findViewById(R.id.layoutAdminProjectUpdates);
        tvNoAdminUpdates = findViewById(R.id.tvNoAdminUpdates);

        setupStatusSpinner();
        loadProjects();

        btnPublishUpdate.setOnClickListener(v -> publishUpdate());
    }

    private void setupStatusSpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statuses
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerUpdateStatus.setAdapter(adapter);
    }

    private void loadProjects() {

        projectListener = firestore.collection("projects")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        Toast.makeText(this,
                                "Failed to load projects: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshot == null) return;

                    String previousProjectId = selectedProjectId;

                    projectList.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        String title = document.getString("title");

                        if (title == null || title.trim().isEmpty()) {
                            title = "Untitled Project";
                        }

                        projectList.add(
                                new ProjectItem(document.getId(), title)
                        );
                    }

                    ArrayAdapter<ProjectItem> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            projectList
                    );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerProjects.setAdapter(adapter);

                    if (projectList.isEmpty()) {
                        selectedProjectId = "";

                        if (updateListener != null) {
                            updateListener.remove();
                            updateListener = null;
                        }

                        layoutAdminProjectUpdates.removeAllViews();
                        tvNoAdminUpdates.setText("No projects available.");
                        tvNoAdminUpdates.setVisibility(View.VISIBLE);
                        return;
                    }

                    int selectedPosition = 0;

                    for (int i = 0; i < projectList.size(); i++) {
                        if (projectList.get(i).id.equals(previousProjectId)) {
                            selectedPosition = i;
                            break;
                        }
                    }

                    spinnerProjects.setSelection(selectedPosition);

                    spinnerProjects.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {

                                @Override
                                public void onItemSelected(
                                        AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id) {

                                    if (position < 0 || position >= projectList.size()) {
                                        return;
                                    }

                                    ProjectItem project = projectList.get(position);

                                    if (!project.id.equals(selectedProjectId)) {
                                        selectedProjectId = project.id;
                                        loadProjectUpdates();
                                    }
                                }

                                @Override
                                public void onNothingSelected(
                                        AdapterView<?> parent) {
                                }
                            }
                    );

                    if (!previousProjectId.isEmpty()
                            && previousProjectId.equals(projectList.get(selectedPosition).id)) {
                        selectedProjectId = previousProjectId;
                        loadProjectUpdates();
                    }
                });
    }

    private void publishUpdate() {

        if (selectedProjectId.isEmpty()) {
            Toast.makeText(this,
                    "Please select a project",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String projectId = selectedProjectId;

        String title = etUpdateTitle.getText().toString().trim();
        String description = etUpdateDescription.getText().toString().trim();
        String status = spinnerUpdateStatus.getSelectedItem().toString();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this,
                    "Please enter title and description",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressPublishUpdate.setVisibility(View.VISIBLE);
        btnPublishUpdate.setEnabled(false);

        Map<String, Object> update = new HashMap<>();
        update.put("title", title);
        update.put("description", description);
        update.put("status", status);
        update.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("projects")
                .document(projectId)
                .collection("updates")
                .add(update)
                .addOnSuccessListener(documentReference -> {

                    progressPublishUpdate.setVisibility(View.GONE);
                    btnPublishUpdate.setEnabled(true);

                    etUpdateTitle.setText("");
                    etUpdateDescription.setText("");

                    Toast.makeText(this,
                            "Project update published successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {

                    progressPublishUpdate.setVisibility(View.GONE);
                    btnPublishUpdate.setEnabled(true);

                    Toast.makeText(this,
                            "Publish failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadProjectUpdates() {

        if (selectedProjectId.isEmpty()) return;

        if (updateListener != null) {
            updateListener.remove();
            updateListener = null;
        }

        layoutAdminProjectUpdates.removeAllViews();
        tvNoAdminUpdates.setVisibility(View.GONE);

        String projectId = selectedProjectId;

        updateListener = firestore.collection("projects")
                .document(projectId)
                .collection("updates")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        Toast.makeText(this,
                                "Failed to load updates: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    layoutAdminProjectUpdates.removeAllViews();

                    if (snapshot == null || snapshot.isEmpty()) {
                        tvNoAdminUpdates.setText("No project updates available.");
                        tvNoAdminUpdates.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvNoAdminUpdates.setVisibility(View.GONE);

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        createUpdateCard(document, projectId);
                    }
                });
    }

    private void createUpdateCard(
            DocumentSnapshot document,
            String projectId) {

        String title = document.getString("title");
        String description = document.getString("description");
        String status = document.getString("status");

        if (title == null) title = "Untitled Update";
        if (description == null) description = "";
        if (status == null) status = "Planned";

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.LTGRAY);

        card.setBackground(background);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        cardParams.setMargins(0, dp(6), 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(Color.BLACK);
        titleView.setTypeface(null, Typeface.BOLD);

        TextView descriptionView = new TextView(this);
        descriptionView.setText(description);
        descriptionView.setTextSize(14);
        descriptionView.setTextColor(Color.DKGRAY);
        descriptionView.setPadding(0, dp(8), 0, dp(8));

        TextView statusView = new TextView(this);
        statusView.setText("Status: " + status);
        statusView.setTextSize(14);
        statusView.setTextColor(Color.rgb(20, 125, 100));

        TextView dateView = new TextView(this);
        dateView.setText("Published: " + formatDate(document.get("createdAt")));
        dateView.setTextSize(12);
        dateView.setTextColor(Color.GRAY);
        dateView.setPadding(0, dp(6), 0, dp(10));

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button editButton = new Button(this);
        editButton.setText("Edit");

        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        buttonParams.setMargins(dp(4), 0, dp(4), 0);

        buttonLayout.addView(editButton, buttonParams);
        buttonLayout.addView(deleteButton, buttonParams);

        card.addView(titleView);
        card.addView(descriptionView);
        card.addView(statusView);
        card.addView(dateView);
        card.addView(buttonLayout);

        editButton.setOnClickListener(v ->
                showEditDialog(document, projectId)
        );

        deleteButton.setOnClickListener(v ->
                confirmDelete(document, projectId)
        );

        layoutAdminProjectUpdates.addView(card);
    }

    private void showEditDialog(
            DocumentSnapshot document,
            String projectId) {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), 0);

        EditText titleInput = new EditText(this);
        titleInput.setHint("Update Title");
        titleInput.setText(document.getString("title"));

        EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("Update Description");
        descriptionInput.setText(document.getString("description"));
        descriptionInput.setMinLines(3);
        descriptionInput.setGravity(Gravity.TOP | Gravity.START);

        Spinner statusSpinner = new Spinner(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statuses
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        statusSpinner.setAdapter(adapter);

        String currentStatus = document.getString("status");

        if (currentStatus != null) {
            for (int i = 0; i < statuses.length; i++) {
                if (statuses[i].equals(currentStatus)) {
                    statusSpinner.setSelection(i);
                    break;
                }
            }
        }

        layout.addView(titleInput);
        layout.addView(descriptionInput);
        layout.addView(statusSpinner);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Project Update")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save Changes", null)
                .create();

        dialog.setOnShowListener(d -> {

            Button saveButton = dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            );

            saveButton.setOnClickListener(v -> {

                String newTitle = titleInput.getText().toString().trim();
                String newDescription =
                        descriptionInput.getText().toString().trim();
                String newStatus =
                        statusSpinner.getSelectedItem().toString();

                if (newTitle.isEmpty() || newDescription.isEmpty()) {
                    Toast.makeText(this,
                            "Title and description cannot be empty",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                saveButton.setEnabled(false);

                Map<String, Object> changes = new HashMap<>();
                changes.put("title", newTitle);
                changes.put("description", newDescription);
                changes.put("status", newStatus);

                firestore.collection("projects")
                        .document(projectId)
                        .collection("updates")
                        .document(document.getId())
                        .update(changes)
                        .addOnSuccessListener(unused -> {

                            Toast.makeText(this,
                                    "Update edited successfully",
                                    Toast.LENGTH_SHORT).show();

                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {

                            saveButton.setEnabled(true);

                            Toast.makeText(this,
                                    "Edit failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            });
        });

        dialog.show();
    }

    private void confirmDelete(
            DocumentSnapshot document,
            String projectId) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Project Update")
                .setMessage("Are you sure you want to delete this update?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {

                    firestore.collection("projects")
                            .document(projectId)
                            .collection("updates")
                            .document(document.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(this,
                                        "Update deleted successfully",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(this,
                                        "Delete failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .show();
    }

    private String formatDate(Object value) {

        if (value == null) return "Recently";

        Date date = null;

        if (value instanceof Timestamp) {
            date = ((Timestamp) value).toDate();
        } else if (value instanceof Date) {
            date = (Date) value;
        } else if (value instanceof String) {
            return (String) value;
        }

        if (date != null) {

            SimpleDateFormat formatter = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            );

            return formatter.format(date);
        }

        return "Recently";
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {

        if (projectListener != null) {
            projectListener.remove();
        }

        if (updateListener != null) {
            updateListener.remove();
        }

        super.onDestroy();
    }

    private static class ProjectItem {

        String id;
        String title;

        ProjectItem(String id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
