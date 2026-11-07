
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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ManageEmergenciesActivity extends AppCompatActivity {

    TextView tvEmergencyCount, tvNoEmergencies;
    ProgressBar progressEmergencies;
    ListView lvEmergencies;
    Button btnBackEmergencies;

    FirebaseFirestore firestore;

    ArrayList<String> emergencyList;
    ArrayList<String> emergencyIds;

    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_emergencies);

        tvEmergencyCount = findViewById(R.id.tvEmergencyCount);
        tvNoEmergencies = findViewById(R.id.tvNoEmergencies);

        progressEmergencies = findViewById(R.id.progressEmergencies);

        lvEmergencies = findViewById(R.id.lvEmergencies);
        btnBackEmergencies = findViewById(R.id.btnBackEmergencies);

        firestore = FirebaseFirestore.getInstance();

        emergencyList = new ArrayList<>();
        emergencyIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                emergencyList
        );

        lvEmergencies.setAdapter(adapter);

        btnBackEmergencies.setOnClickListener(v -> finish());

        lvEmergencies.setOnItemClickListener(
                (parent, view, position, id) -> {

                    String emergencyId = emergencyIds.get(position);

                    android.content.Intent intent =
                            new android.content.Intent(
                                    ManageEmergenciesActivity.this,
                                    EmergencyAdminDetailsActivity.class
                            );

                    intent.putExtra("EMERGENCY_ID", emergencyId);

                    startActivity(intent);
                }
        );

        loadEmergencies();
    }

    private void loadEmergencies() {

        progressEmergencies.setVisibility(View.VISIBLE);
        tvNoEmergencies.setVisibility(View.GONE);

        firestore.collection("emergencies")
                .get()
                .addOnSuccessListener(snapshot -> {

                    emergencyList.clear();
                    emergencyIds.clear();

                    for (QueryDocumentSnapshot document : snapshot) {

                        String name = document.getString("name");
                        String phone = document.getString("phone");
                        String type = document.getString("type");
                        String location = document.getString("location");
                        String description =
                                document.getString("description");
                        String status = document.getString("status");

                        emergencyIds.add(document.getId());

                        emergencyList.add(
                                "Emergency Type: " + type +
                                        "\nName: " + name +
                                        "\nPhone: " + phone +
                                        "\nLocation: " + location +
                                        "\nDescription: " + description +
                                        "\nStatus: " + status
                        );
                    }

                    adapter.notifyDataSetChanged();

                    tvEmergencyCount.setText(
                            "Total Emergencies: " + emergencyList.size()
                    );

                    progressEmergencies.setVisibility(View.GONE);

                    if (emergencyList.isEmpty()) {
                        tvNoEmergencies.setVisibility(View.VISIBLE);
                    }

                })
                .addOnFailureListener(e -> {

                    progressEmergencies.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Failed to load emergencies: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (firestore != null) {
            loadEmergencies();
        }
    }
}
