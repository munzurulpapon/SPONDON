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

public class EmergencyAdminDetailsActivity extends AppCompatActivity {

    TextView tvEmergencyName, tvEmergencyPhone;
    TextView tvEmergencyType, tvEmergencyLocation;
    TextView tvEmergencyDescription, tvEmergencyCurrentStatus;

    Spinner spEmergencyStatus;
    EditText etEmergencyAdminNote;

    Button btnUpdateEmergency, btnBackEmergencyDetail;

    FirebaseFirestore firestore;

    String emergencyId;
    String currentStatus = "Pending";

    String[] statuses = {
            "Pending",
            "Accepted",
            "In Progress",
            "Resolved",
            "Rejected"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_admin_details);

        tvEmergencyName = findViewById(R.id.tvEmergencyName);
        tvEmergencyPhone = findViewById(R.id.tvEmergencyPhone);
        tvEmergencyType = findViewById(R.id.tvEmergencyType);
        tvEmergencyLocation = findViewById(R.id.tvEmergencyLocation);
        tvEmergencyDescription = findViewById(R.id.tvEmergencyDescription);
        tvEmergencyCurrentStatus = findViewById(R.id.tvEmergencyCurrentStatus);

        spEmergencyStatus = findViewById(R.id.spEmergencyStatus);
        etEmergencyAdminNote = findViewById(R.id.etEmergencyAdminNote);

        btnUpdateEmergency = findViewById(R.id.btnUpdateEmergency);
        btnBackEmergencyDetail = findViewById(R.id.btnBackEmergencyDetail);

        firestore = FirebaseFirestore.getInstance();

        emergencyId = getIntent().getStringExtra("EMERGENCY_ID");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statuses
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spEmergencyStatus.setAdapter(adapter);

        btnBackEmergencyDetail.setOnClickListener(v -> finish());

        btnUpdateEmergency.setOnClickListener(v -> updateEmergency());

        loadEmergencyDetails();
    }

    private void loadEmergencyDetails() {

        if (emergencyId == null || emergencyId.isEmpty()) {
            Toast.makeText(this, "Emergency ID not found",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("emergencies")
                .document(emergencyId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(this, "Emergency not found",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String name = document.getString("name");
                    String phone = document.getString("phone");
                    String type = document.getString("type");
                    String location = document.getString("location");
                    String description = document.getString("description");

                    currentStatus = document.getString("status");

                    tvEmergencyName.setText("Name: " + name);
                    tvEmergencyPhone.setText("Phone: " + phone);
                    tvEmergencyType.setText("Emergency Type: " + type);
                    tvEmergencyLocation.setText("Location: " + location);
                    tvEmergencyDescription.setText(
                            "Description: " + description
                    );

                    tvEmergencyCurrentStatus.setText(
                            "Current Status: " + currentStatus
                    );

                    int position = 0;

                    for (int i = 0; i < statuses.length; i++) {
                        if (statuses[i].equals(currentStatus)) {
                            position = i;
                            break;
                        }
                    }

                    spEmergencyStatus.setSelection(position);

                    String adminNote = document.getString("adminNote");

                    if (adminNote != null) {
                        etEmergencyAdminNote.setText(adminNote);
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

    private void updateEmergency() {

        String newStatus = spEmergencyStatus.getSelectedItem().toString();

        String adminNote = etEmergencyAdminNote.getText()
                .toString().trim();

        if (emergencyId == null) {
            return;
        }

        btnUpdateEmergency.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();

        updates.put("status", newStatus);
        updates.put("adminNote", adminNote);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("emergencies")
                .document(emergencyId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    // Realtime Database: push live status to the citizen
                    RealtimeDbHelper.updateEmergencyStatus(emergencyId, newStatus, adminNote);

                    btnUpdateEmergency.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Emergency status updated!",
                            Toast.LENGTH_SHORT
                    ).show();

                    currentStatus = newStatus;

                    tvEmergencyCurrentStatus.setText(
                            "Current Status: " + currentStatus
                    );
                })
                .addOnFailureListener(e -> {

                    btnUpdateEmergency.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}