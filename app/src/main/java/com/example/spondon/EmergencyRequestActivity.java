
package com.example.spondon;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmergencyRequestActivity extends AppCompatActivity {

    EditText etEmergencyName, etEmergencyPhone;
    EditText etEmergencyLocation, etEmergencyDescription;

    Spinner spEmergencyType;

    Button btnSubmitEmergency, btnBackEmergency;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String[] emergencyTypes = {
            "Medical Emergency",
            "Fire Emergency",
            "Road Accident",
            "Police Assistance",
            "Rescue Assistance",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_request);

        etEmergencyName = findViewById(R.id.etEmergencyName);
        etEmergencyPhone = findViewById(R.id.etEmergencyPhone);
        etEmergencyLocation = findViewById(R.id.etEmergencyLocation);
        etEmergencyDescription =
                findViewById(R.id.etEmergencyDescription);

        spEmergencyType = findViewById(R.id.spEmergencyType);

        btnSubmitEmergency = findViewById(R.id.btnSubmitEmergency);
        btnBackEmergency = findViewById(R.id.btnBackEmergency);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                emergencyTypes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spEmergencyType.setAdapter(adapter);

        btnBackEmergency.setOnClickListener(v -> finish());

        btnSubmitEmergency.setOnClickListener(v -> submitEmergency());
    }

    private void submitEmergency() {

        String name = etEmergencyName.getText().toString().trim();
        String phone = etEmergencyPhone.getText().toString().trim();

        String type = spEmergencyType.getSelectedItem().toString();

        String location =
                etEmergencyLocation.getText().toString().trim();

        String description =
                etEmergencyDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etEmergencyName.setError("Enter your name");
            return;
        }

        if (phone.isEmpty()) {
            etEmergencyPhone.setError("Enter your phone number");
            return;
        }

        if (location.isEmpty()) {
            etEmergencyLocation.setError("Enter emergency location");
            return;
        }

        if (description.isEmpty()) {
            etEmergencyDescription.setError(
                    "Describe the emergency"
            );
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnSubmitEmergency.setEnabled(false);

        String uid = firebaseAuth.getCurrentUser().getUid();

        Map<String, Object> emergency = new HashMap<>();

        emergency.put("uid", uid);
        emergency.put("name", name);
        emergency.put("phone", phone);
        emergency.put("type", type);
        emergency.put("location", location);
        emergency.put("description", description);
        emergency.put("status", "Pending");
        emergency.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("emergencies")
                .add(emergency)
                .addOnSuccessListener(documentReference -> {

                    btnSubmitEmergency.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Emergency request submitted!",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnSubmitEmergency.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Submission failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
