
package com.example.spondon;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ManageServicesActivity extends AppCompatActivity {

    EditText etServiceName, etServiceCategory, etServiceDescription;
    Button btnAddService, btnBackServices;
    TextView tvServiceCount;
    ListView lvServices;

    FirebaseFirestore firestore;
    FirebaseAuth firebaseAuth;

    ArrayList<String> serviceList;
    ArrayList<String> serviceIds;

    ArrayAdapter<String> adapter;

    String selectedServiceId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_services);

        etServiceName = findViewById(R.id.etServiceName);
        etServiceCategory = findViewById(R.id.etServiceCategory);
        etServiceDescription = findViewById(R.id.etServiceDescription);

        btnAddService = findViewById(R.id.btnAddService);
        btnBackServices = findViewById(R.id.btnBackServices);

        tvServiceCount = findViewById(R.id.tvServiceCount);
        lvServices = findViewById(R.id.lvServices);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        serviceList = new ArrayList<>();
        serviceIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                serviceList
        );

        lvServices.setAdapter(adapter);

        btnAddService.setOnClickListener(v -> addService());

        btnBackServices.setOnClickListener(v -> finish());

        lvServices.setOnItemClickListener((parent, view, position, id) -> {
            selectedServiceId = serviceIds.get(position);
            showServiceOptions(position);
        });

        loadServices();
    }

    private void addService() {

        String name = etServiceName.getText().toString().trim();
        String category = etServiceCategory.getText().toString().trim();
        String description =
                etServiceDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etServiceName.setError("Enter service name");
            return;
        }

        if (category.isEmpty()) {
            etServiceCategory.setError("Enter service category");
            return;
        }

        if (description.isEmpty()) {
            etServiceDescription.setError("Enter service description");
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddService.setEnabled(false);

        Map<String, Object> service = new HashMap<>();
        service.put("name", name);
        service.put("category", category);
        service.put("description", description);
        service.put("createdBy", firebaseAuth.getCurrentUser().getUid());
        service.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("services")
                .add(service)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(
                            this,
                            "Service added successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    clearFields();
                    btnAddService.setEnabled(true);
                    loadServices();
                })
                .addOnFailureListener(e -> {

                    btnAddService.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadServices() {

        firestore.collection("services")
                .get()
                .addOnSuccessListener(snapshot -> {

                    serviceList.clear();
                    serviceIds.clear();

                    for (QueryDocumentSnapshot document :
                            snapshot) {

                        String name = document.getString("name");
                        String category =
                                document.getString("category");
                        String description =
                                document.getString("description");

                        serviceIds.add(document.getId());

                        serviceList.add(
                                "Service: " + name +
                                        "\nCategory: " + category +
                                        "\nDescription: " + description
                        );
                    }

                    adapter.notifyDataSetChanged();

                    tvServiceCount.setText(
                            "Total Services: " + serviceList.size()
                    );
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Unable to load services: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void showServiceOptions(int position) {

        String[] options = {"Edit Service", "Delete Service"};

        new AlertDialog.Builder(this)
                .setTitle("Manage Service")
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {
                        editService(position);
                    } else {
                        confirmDelete();
                    }
                })
                .show();
    }

    private void editService(int position) {

        String serviceId = serviceIds.get(position);

        firestore.collection("services")
                .document(serviceId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        return;
                    }

                    EditText input = new EditText(this);
                    input.setText(document.getString("name"));
                    input.setInputType(
                            InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    );

                    new AlertDialog.Builder(this)
                            .setTitle("Edit Service Name")
                            .setView(input)
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Save", (dialog, which) -> {

                                String newName =
                                        input.getText().toString().trim();

                                if (newName.isEmpty()) {
                                    Toast.makeText(
                                            this,
                                            "Service name cannot be empty",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    return;
                                }

                                firestore.collection("services")
                                        .document(serviceId)
                                        .update("name", newName)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(
                                                    this,
                                                    "Service updated!",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                            loadServices();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(
                                                        this,
                                                        "Update failed: " +
                                                                e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show()
                                        );
                            })
                            .show();
                });
    }

    private void confirmDelete() {

        new AlertDialog.Builder(this)
                .setTitle("Delete Service")
                .setMessage(
                        "Are you sure you want to delete this service?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {

                    firestore.collection("services")
                            .document(selectedServiceId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(
                                        this,
                                        "Service deleted!",
                                        Toast.LENGTH_SHORT
                                ).show();
                                loadServices();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Delete failed: " +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .show();
    }

    private void clearFields() {
        etServiceName.setText("");
        etServiceCategory.setText("");
        etServiceDescription.setText("");
    }
}
