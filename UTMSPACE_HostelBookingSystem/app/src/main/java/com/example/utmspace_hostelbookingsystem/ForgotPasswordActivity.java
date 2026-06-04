package com.example.utmspace_hostelbookingsystem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;
import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnSendResetLink;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    // Password validation pattern: at least 7 characters, must contain both letters and numbers
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{7,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        // 2. Initialize UI controls - 確保 XML 中有這些 ID
        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetLink = findViewById(R.id.sendResetBtn);
        tvBackToLogin = findViewById(R.id.backToLogin);

        // 3. Initialize Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending reset link...");
        progressDialog.setCancelable(false);

        // 4. Add auto lowercase for email input
        setupEmailAutoLowercase();

        // 5. Send reset email button click event
        btnSendResetLink.setOnClickListener(v -> resetPassword());

        // 6. Back to login page
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void setupEmailAutoLowercase() {
        if (etResetEmail != null) {
            etResetEmail.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No action needed
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // No action needed
                }

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
    }

    private void resetPassword() {
        String email = etResetEmail.getText().toString().trim().toLowerCase(Locale.ROOT);

        if (TextUtils.isEmpty(email)) {
            etResetEmail.setError("Email is required");
            etResetEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etResetEmail.setError("Please enter a valid email address");
            etResetEmail.requestFocus();
            return;
        }

        progressDialog.show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Reset link sent to: " + email,
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Error: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Public method to validate password (can be called from other activities)
    public static boolean isPasswordValid(String password) {
        if (TextUtils.isEmpty(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    // Get password requirements as a string
    public static String getPasswordRequirements() {
        return "Password must be at least 7 characters and contain both letters and numbers";
    }
}