package com.example.spondon;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BookServiceActivity extends AppCompatActivity {

    private TextView tvBookServiceName;
    private EditText etCustomerName, etCustomerPhone, etCustomerAddress, etPreferredDate, etServiceNotes;
    private Spinner spPreferredTime;
    private Button btnSubmitBooking, btnCancelBooking;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private DatabaseHelper databaseHelper;

    private String serviceId = "";
    private String serviceName = "Smart City Service";
    private String serviceCategory = "General";

    private final String[] timeSlots = {
            "Morning (09:00 AM - 12:00 PM)",
            "Afternoon (12:00 PM - 04:00 PM)",
            "Evening (04:00 PM - 08:00 PM)",
            "Emergency Immediate (Next Available)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_service);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(this);

        tvBookServiceName = findViewById(R.id.tvBookServiceName);
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        etCustomerAddress = findViewById(R.id.etCustomerAddress);
        etPreferredDate = findViewById(R.id.etPreferredDate);
        spPreferredTime = findViewById(R.id.spPreferredTime);
        etServiceNotes = findViewById(R.id.etServiceNotes);

        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);

        String idExtra = getIntent().getStringExtra("SERVICE_ID");
        String nameExtra = getIntent().getStringExtra("SERVICE_NAME");
        String catExtra = getIntent().getStringExtra("SERVICE_CATEGORY");

        if (idExtra != null) serviceId = idExtra;
        if (nameExtra != null) serviceName = nameExtra;
        if (catExtra != null) serviceCategory = catExtra;

        tvBookServiceName.setText(serviceName + " (" + serviceCategory + ")");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                timeSlots
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPreferredTime.setAdapter(adapter);

        // Pre-fill user name and phone if available in Firebase
        if (firebaseAuth.getCurrentUser() != null) {
            String uid = firebaseAuth.getCurrentUser().getUid();
            firestore.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    if (name != null) etCustomerName.setText(name);
                    if (phone != null) etCustomerPhone.setText(phone);
                }
            });
        }

        btnCancelBooking.setOnClickListener(v -> finish());
        btnSubmitBooking.setOnClickListener(v -> submitBooking());
    }

    private void submitBooking() {
        String name = etCustomerName.getText().toString().trim();
        String phone = etCustomerPhone.getText().toString().trim();
        String address = etCustomerAddress.getText().toString().trim();
        String date = etPreferredDate.getText().toString().trim();
        String time = spPreferredTime.getSelectedItem().toString();
        String notes = etServiceNotes.getText().toString().trim();

        if (name.isEmpty()) {
            etCustomerName.setError("Enter your name");
            return;
        }

        if (phone.isEmpty()) {
            etCustomerPhone.setError("Enter phone number");
            return;
        }

        if (address.isEmpty()) {
            etCustomerAddress.setError("Enter service address");
            return;
        }

        if (date.isEmpty()) {
            date = "Next Available Date";
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first to submit a booking", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();
        btnSubmitBooking.setEnabled(false);

        // Save locally to SQLite first
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("service_name", serviceName);
        cv.put("category", serviceCategory);
        cv.put("address", address);
        cv.put("phone", phone);
        cv.put("preferred_date", date);
        cv.put("preferred_time", time);
        cv.put("notes", notes);
        cv.put("status", "Pending");
        long localId = db.insert(DatabaseHelper.TABLE_SERVICE_REQUESTS, null, cv);
        db.close();

        // Prepare Firestore payload
        Map<String, Object> request = new HashMap<>();
        request.put("serviceId", serviceId);
        request.put("serviceName", serviceName);
        request.put("category", serviceCategory);
        request.put("citizenUid", uid);
        request.put("citizenName", name);
        request.put("citizenPhone", phone);
        request.put("address", address);
        request.put("preferredDate", date);
        request.put("preferredTime", time);
        request.put("notes", notes);
        request.put("status", "Pending");
        request.put("rating", 0);
        request.put("feedback", "");
        request.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("service_requests")
                .add(request)
                .addOnSuccessListener(docRef -> {
                    btnSubmitBooking.setEnabled(true);

                    // Update SQLite with firebase id
                    SQLiteDatabase updateDb = databaseHelper.getWritableDatabase();
                    ContentValues ucv = new ContentValues();
                    ucv.put("firebase_id", docRef.getId());
                    updateDb.update(DatabaseHelper.TABLE_SERVICE_REQUESTS, ucv, "id = ?", new String[]{String.valueOf(localId)});
                    updateDb.close();

                    Toast.makeText(this, "Service booked successfully! A provider will contact you shortly.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitBooking.setEnabled(true);
                    Toast.makeText(this, "Booking saved locally, but online sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}
