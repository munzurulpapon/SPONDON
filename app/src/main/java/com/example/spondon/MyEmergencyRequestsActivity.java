package com.example.spondon;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MyEmergencyRequestsActivity extends AppCompatActivity {

    private TextView tvEmergencyHistoryTitle, tvNoEmergencyHistory;
    private ProgressBar progressEmergencyHistory;
    private ListView lvEmergencyHistory;
    private Button btnBackEmergencyHistory;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private DatabaseHelper databaseHelper;

    private ArrayList<String> emergencyList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_emergency_requests);

        tvEmergencyHistoryTitle = findViewById(R.id.tvEmergencyHistoryTitle);
        tvNoEmergencyHistory = findViewById(R.id.tvNoEmergencyHistory);
        progressEmergencyHistory = findViewById(R.id.progressEmergencyHistory);
        lvEmergencyHistory = findViewById(R.id.lvEmergencyHistory);
        btnBackEmergencyHistory = findViewById(R.id.btnBackEmergencyHistory);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        databaseHelper = new DatabaseHelper(this);

        emergencyList = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                emergencyList
        );

        lvEmergencyHistory.setAdapter(adapter);

        btnBackEmergencyHistory.setOnClickListener(v -> finish());

        loadEmergencyHistory();
    }

    private void loadEmergencyHistory() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressEmergencyHistory.setVisibility(View.VISIBLE);
        tvNoEmergencyHistory.setVisibility(View.GONE);

        String uid = auth.getCurrentUser().getUid();

        // 1. First fetch remote from Firestore
        firestore.collection("emergencies")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    emergencyList.clear();

                    for (QueryDocumentSnapshot document : snapshot) {
                        String type = document.getString("type");
                        String location = document.getString("location");
                        String description = document.getString("description");
                        String status = document.getString("status");
                        String adminNote = document.getString("adminNote");

                        emergencyList.add(
                                "🚨 Emergency: " + type +
                                        "\n📍 Location: " + location +
                                        "\n📝 Details: " + description +
                                        "\n⚡ Status: " + (status != null ? status : "Pending") +
                                        "\n📋 Municipal Note: " +
                                        (adminNote == null || adminNote.isEmpty()
                                                ? "Pending response team"
                                                : adminNote)
                        );
                    }

                    // Also check local SQLite records if any were saved offline
                    loadLocalEmergencyRequests(uid);

                    adapter.notifyDataSetChanged();
                    tvEmergencyHistoryTitle.setText("My Emergency Requests (" + emergencyList.size() + ")");
                    progressEmergencyHistory.setVisibility(View.GONE);

                    if (emergencyList.isEmpty()) {
                        tvNoEmergencyHistory.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to local SQLite storage
                    emergencyList.clear();
                    loadLocalEmergencyRequests(uid);

                    adapter.notifyDataSetChanged();
                    tvEmergencyHistoryTitle.setText("My Emergency Requests (Local: " + emergencyList.size() + ")");
                    progressEmergencyHistory.setVisibility(View.GONE);

                    if (emergencyList.isEmpty()) {
                        tvNoEmergencyHistory.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Showing locally saved emergency history (Offline)", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadLocalEmergencyRequests(String uid) {
        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT type, location, description, status, created_at FROM " +
                            DatabaseHelper.TABLE_EMERGENCY_REQUESTS +
                            " WHERE uid = ? ORDER BY id DESC",
                    new String[]{uid}
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String type = cursor.getString(0);
                    String location = cursor.getString(1);
                    String desc = cursor.getString(2);
                    String status = cursor.getString(3);
                    String createdAt = cursor.getString(4);

                    String entry = "🚨 Emergency: " + type + " [Saved Locally]" +
                            "\n📍 Location: " + location +
                            "\n📝 Details: " + desc +
                            "\n⚡ Status: " + status +
                            "\n🕒 Time: " + createdAt;

                    // Avoid duplicate display if already pulled from Firestore
                    boolean alreadyPresent = false;
                    for (String existing : emergencyList) {
                        if (desc != null && type != null
                                && existing.contains(desc) && existing.contains(type)) {
                            alreadyPresent = true;
                            break;
                        }
                    }

                    if (!alreadyPresent) {
                        emergencyList.add(entry);
                    }
                }
                cursor.close();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firestore != null && auth != null) {
            loadEmergencyHistory();
        }
    }
}