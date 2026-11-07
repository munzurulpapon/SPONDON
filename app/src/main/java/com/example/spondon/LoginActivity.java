
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

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    RadioGroup roleGroup;

    Button btnLogin, btnRegister;
    TextView tvForgotPassword;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        roleGroup = findViewById(R.id.roleGroup);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) {
            etEmail.setError("Enter your email");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Enter your password");
            etPassword.requestFocus();
            return;
        }

        int selectedRole = roleGroup.getCheckedRadioButtonId();

        String selectedRoleName;

        if (selectedRole == R.id.rbCitizen) {
            selectedRoleName = "Citizen";
        } else if (selectedRole == R.id.rbProvider) {
            selectedRoleName = "Service Provider";
        } else if (selectedRole == R.id.rbAdmin) {
            selectedRoleName = "Admin";
        } else {
            Toast.makeText(this, "Please select a role",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        btnLogin.setEnabled(true);

                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";

                        Toast.makeText(this, error,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String userId = firebaseAuth.getCurrentUser().getUid();

                    firestore.collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener(document -> {

                                btnLogin.setEnabled(true);

                                if (!document.exists()) {
                                    firebaseAuth.signOut();

                                    Toast.makeText(this,
                                            "User profile not found!",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                String actualRole = document.getString("role");

                                if (actualRole == null
                                        || !actualRole.equals(selectedRoleName)) {

                                    firebaseAuth.signOut();

                                    Toast.makeText(this,
                                            "Selected role does not match your account!",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                Toast.makeText(this,
                                        "Login successful!",
                                        Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(
                                        LoginActivity.this,
                                        DashboardActivity.class
                                );

                                intent.putExtra("USER_ROLE", actualRole);

                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {

                                firebaseAuth.signOut();
                                btnLogin.setEnabled(true);

                                Toast.makeText(this,
                                        "Unable to load user profile: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void resetPassword() {

        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Enter your email first");
            etEmail.requestFocus();
            return;
        }

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Password reset link sent to your email",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unable to send reset email";

                        Toast.makeText(this, error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
