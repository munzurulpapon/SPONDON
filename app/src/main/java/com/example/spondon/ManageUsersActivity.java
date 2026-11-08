
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

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    TextView tvUserCount, tvNoUsers;
    ProgressBar progressUsers;
    ListView lvUsers;
    Button btnBackUsers;

    FirebaseFirestore firestore;
    FirebaseAuth firebaseAuth;

    ArrayList<String> userList;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        tvUserCount = findViewById(R.id.tvUserCount);
        tvNoUsers = findViewById(R.id.tvNoUsers);
        progressUsers = findViewById(R.id.progressUsers);
        lvUsers = findViewById(R.id.lvUsers);
        btnBackUsers = findViewById(R.id.btnBackUsers);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        userList = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                userList
        );

        lvUsers.setAdapter(adapter);

        btnBackUsers.setOnClickListener(v -> finish());

        loadUsers();
    }

    private void loadUsers() {

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "Please login first",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        progressUsers.setVisibility(View.VISIBLE);
        tvNoUsers.setVisibility(View.GONE);
        lvUsers.setVisibility(View.GONE);

        firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    progressUsers.setVisibility(View.GONE);

                    userList.clear();

                    int totalUsers = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        String name = document.getString("name");
                        String email = document.getString("email");
                        String phone = document.getString("phone");
                        String role = document.getString("role");

                        if (name == null) name = "Unknown";
                        if (email == null) email = "Not available";
                        if (phone == null) phone = "Not available";
                        if (role == null) role = "Unknown";

                        // Display only Citizen and Service Provider
                        if (role.equals("Citizen")
                                || role.equals("Service Provider")) {

                            String userDetails =
                                    "Name: " + name +
                                            "\nEmail: " + email +
                                            "\nPhone: " + phone +
                                            "\nRole: " + role;

                            userList.add(userDetails);
                            totalUsers++;
                        }
                    }

                    tvUserCount.setText(
                            "Total Registered Users: " + totalUsers
                    );

                    adapter.notifyDataSetChanged();

                    if (userList.isEmpty()) {
                        tvNoUsers.setVisibility(View.VISIBLE);
                        lvUsers.setVisibility(View.GONE);
                    } else {
                        tvNoUsers.setVisibility(View.GONE);
                        lvUsers.setVisibility(View.VISIBLE);
                    }

                })
                .addOnFailureListener(e -> {

                    progressUsers.setVisibility(View.GONE);

                    Toast.makeText(
                            ManageUsersActivity.this,
                            "Failed to load users: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
