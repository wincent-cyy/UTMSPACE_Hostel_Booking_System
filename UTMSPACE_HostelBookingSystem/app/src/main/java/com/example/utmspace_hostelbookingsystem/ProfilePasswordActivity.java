package com.example.utmspace_hostelbookingsystem;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

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
            setContentView(R.layout.activity_profile_password);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Layout Inflation Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupEmailAutoLowercase();  // ✅ 添加自动转小写
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
    }

    // ✅ 添加：自动将邮箱转换为小写
    private void setupEmailAutoLowercase() {
        if (etResetEmail == null) return;

        etResetEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null) {
                    String input = s.toString();
                    String lowerCaseInput = input.toLowerCase(Locale.ROOT);
                    if (!input.equals(lowerCaseInput)) {
                        etResetEmail.removeTextChangedListener(this);
                        etResetEmail.setText(lowerCaseInput);
                        etResetEmail.setSelection(lowerCaseInput.length());
                        etResetEmail.addTextChangedListener(this);
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
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

        String email = etResetEmail.getText().toString().trim().toLowerCase(Locale.ROOT);

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
                        // ✅ 添加密码要求提示
                        Toast.makeText(ProfilePasswordActivity.this,
                                "Reset link sent! Please check your email inbox.\n\nNote: New password must be at least 7 characters and contain both letters and numbers",
                                Toast.LENGTH_LONG).show();

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