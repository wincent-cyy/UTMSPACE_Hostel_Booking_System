package com.example.utmspace_hostelbookingsystem;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ProfilePasswordActivity extends AppCompatActivity {

    // View Component Bindings
    private FrameLayout btnBack;
    private EditText etResetEmail;
    private Button btnSendResetLink;

    // Firebase Authentication Configuration
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // 1. Explicitly inflate the XML Layout view resource
            setContentView(R.layout.activity_profile_password);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Layout Inflation Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. Initialize Firebase Components Safely
        mAuth = FirebaseAuth.getInstance();

        // 3. Map Layout View Component Hooks
        initViews();

        // 4. Setup Interactive Click Framework Listeners
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
    }

    private void setupClickListeners() {
        // Safe check to verify component injection before binding listeners
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSendResetLink != null) {
            btnSendResetLink.setOnClickListener(v -> handlePasswordReset());
        }
    }

    private void handlePasswordReset() {
        if (etResetEmail == null || btnSendResetLink == null) {
            Toast.makeText(this, "Views are not properly initialized.", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = etResetEmail.getText().toString().trim();

        // 1. Form Validation Checks
        if (email.isEmpty()) {
            etResetEmail.setError("Email address is required");
            etResetEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etResetEmail.setError("Please enter a valid email address");
            etResetEmail.requestFocus();
            return;
        }

        // Disable button temporarily to prevent duplicate network requests
        btnSendResetLink.setEnabled(false);

        // 2. Trigger Firebase Auth Password Recovery Routine
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    // Re-enable the button once the network response finishes safely
                    if (btnSendResetLink != null) {
                        btnSendResetLink.setEnabled(true);
                    }

                    if (task.isSuccessful()) {
                        Toast.makeText(ProfilePasswordActivity.this,
                                "Reset link sent! Please check your email inbox.",
                                Toast.LENGTH_LONG).show();

                        // Terminate current view window and return smoothly
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred.";
                        Toast.makeText(ProfilePasswordActivity.this,
                                "Error: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}