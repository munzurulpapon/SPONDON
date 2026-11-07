
package com.example.spondon;

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

    TextView tvEmergencyHistoryTitle, tvNoEmergencyHistory;
    ProgressBar progressEmergencyHistory;
    ListView lvEmergencyHistory;
    Button btnBackEmergencyHistory;

    FirebaseFirestore firestore;
    FirebaseAuth auth;

    ArrayList<String> emergencyList;
    ArrayAdapter<String> adapter;

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
                                "Emergency Type: " + type +
                                        "\nLocation: " + location +
                                        "\nDescription: " + description +
                                        "\nStatus: " + status +
                                        "\nAdmin Note: " +
                                        (adminNote == null || adminNote.isEmpty()
                                                ? "No admin note yet"
                                                : adminNote)
                        );
                    }

                    adapter.notifyDataSetChanged();

                    tvEmergencyHistoryTitle.setText(
                            "My Emergency Requests (" + emergencyList.size() + ")"
                    );

                    progressEmergencyHistory.setVisibility(View.GONE);

                    if (emergencyList.isEmpty()) {
                        tvNoEmergencyHistory.setVisibility(View.VISIBLE);
                    }

                })
                .addOnFailureListener(e -> {

                    progressEmergencyHistory.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Failed to load requests: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (firestore != null && auth != null) {
            loadEmergencyHistory();
        }
    }
}
