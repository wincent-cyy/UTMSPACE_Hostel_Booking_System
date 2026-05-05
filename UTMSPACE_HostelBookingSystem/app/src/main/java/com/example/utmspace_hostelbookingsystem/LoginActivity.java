package com.example.utmspace_hostelbookingsystem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity_Debug";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvGoToSignUp;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;

    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize Realtime Database with specific region URL
        String databaseUrl = "https://utmspace-hostel-booking-system-default-rtdb.asia-southeast1.firebasedatabase.app/";

        try {
            mDatabase = FirebaseDatabase.getInstance(databaseUrl).getReference();
        } catch (Exception e) {
            Log.e(TAG, "Database URL Error: " + e.getMessage());
            mDatabase = FirebaseDatabase.getInstance().getReference();
        }

        // 3. Initialize UI Components
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        // 4. Click Listeners
        tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        // IMPROVED: Navigation to Forgot Password Page
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        // Enforce lowercase for email processing to ensure consistency with DB
        String email = etEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        progressDialog.show();

        // Setup Timeout logic
        timeoutRunnable = () -> {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                Toast.makeText(this, "Login timeout. Check connection.", Toast.LENGTH_LONG).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 15000);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Check if Email is Verified
                        if (user != null && user.isEmailVerified()) {
                            Log.d(TAG, "Auth successful & Email verified. Checking role...");
                            checkUserRole();
                        } else {
                            // User is authenticated but NOT verified
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            progressDialog.dismiss();

                            mAuth.signOut(); // Force sign out
                            Toast.makeText(LoginActivity.this, "Please verify your email before logging in. Check your inbox.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        progressDialog.dismiss();
                        Log.e(TAG, "Auth failed: " + task.getException());
                        Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                timeoutHandler.removeCallbacks(timeoutRunnable);

                if (isFinishing() || isDestroyed()) return;
                progressDialog.dismiss();

                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    Log.d(TAG, "Role found: " + role);
                    if (role != null) {
                        navigateToDashboard(role);
                    } else {
                        Toast.makeText(LoginActivity.this, "Role not assigned", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "UID not found in DB: " + uid);
                    Toast.makeText(LoginActivity.this, "User data not found in DB", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                progressDialog.dismiss();
                Log.e(TAG, "DB Read Cancelled: " + error.getMessage());
                Toast.makeText(LoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        Log.d(TAG, "Navigating to: " + role);

        switch (role) {
            case "Student":
                intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                break;
            case "Staff":
                intent = new Intent(LoginActivity.this, StaffDashboardActivity.class);
                break;
            case "Technician":
                intent = new Intent(LoginActivity.this, TechnicianDashboardActivity.class);
                break;
            case "Admin":
                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                break;
            default:
                Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                return;
        }

        // Clear activity stack so user cannot go back to login with 'back' button
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler to prevent memory leaks
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }
}