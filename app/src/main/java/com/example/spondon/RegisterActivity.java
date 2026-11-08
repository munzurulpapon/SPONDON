
package com.example.spondon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPhone;
    EditText etPassword, etConfirmPassword;

    RadioGroup registerRoleGroup;

    Button btnCreateAccount;
    TextView tvGoToLogin;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        registerRoleGroup = findViewById(R.id.registerRoleGroup);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnCreateAccount.setOnClickListener(v -> registerUser());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(
                    RegisterActivity.this,
                    LoginActivity.class
            ));
            finish();
        });
    }

    private void registerUser() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        int selectedRole = registerRoleGroup.getCheckedRadioButtonId();

        String role;

        if (selectedRole == R.id.registerCitizen) {
            role = "Citizen";
        } else if (selectedRole == R.id.registerProvider) {
            role = "Service Provider";
        } else {
            Toast.makeText(this, "Please select a role",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateAccount.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        btnCreateAccount.setEnabled(true);

                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";

                        Toast.makeText(this, error,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String userId = firebaseAuth.getCurrentUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", userId);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("phone", phone);
                    user.put("role", role);

                    firestore.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener(unused -> {

                                firebaseAuth.signOut();
                                btnCreateAccount.setEnabled(true);

                                Toast.makeText(this,
                                        "Registration successful! Please login.",
                                        Toast.LENGTH_LONG).show();

                                Intent intent = new Intent(
                                        RegisterActivity.this,
                                        LoginActivity.class
                                );

                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {

                                btnCreateAccount.setEnabled(true);

                                Toast.makeText(this,
                                        "Account created, but profile saving failed: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                });
    }
}
