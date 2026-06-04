package com.example.utmspace_hostelbookingsystem;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;
import java.util.regex.Pattern;

public class ProfilePasswordActivity extends AppCompatActivity {

    // View Component Bindings - Updated for new XML
    private LinearLayout ivBack;
    private TextInputEditText etCurrentPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private TextView btnUpdatePassword;
    private TextView backToProfile;

    // Firebase Authentication Configuration
    private FirebaseAuth mAuth;

    // Password validation pattern: at least 7 characters, must contain both letters and numbers
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{7,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_password);

        mAuth = FirebaseAuth.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
    }

    private void setupClickListeners() {
        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Update Password button
        btnUpdatePassword.setOnClickListener(v -> handlePasswordUpdate());
    }

    private void handlePasswordUpdate() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 1. Validate current password
        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return;
        }

        // 2. Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("New password is required");
            etNewPassword.requestFocus();
            return;
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            etNewPassword.setError("Password must be at least 7 characters and contain both letters and numbers");
            etNewPassword.requestFocus();
            return;
        }

        // 3. Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your new password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // 4. Check if new password is same as current password
        if (currentPassword.equals(newPassword)) {
            Toast.makeText(this, "New password cannot be the same as current password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button temporarily to prevent duplicate requests
        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Updating...");

        // 5. Re-authenticate and update password
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            btnUpdatePassword.setEnabled(true);
            btnUpdatePassword.setText("Update Password");
            return;
        }

        // Re-authenticate user before changing password
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(reAuthTask -> {
                    if (reAuthTask.isSuccessful()) {
                        // Re-authentication successful, update password
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    btnUpdatePassword.setEnabled(true);
                                    btnUpdatePassword.setText("Update Password");

                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ProfilePasswordActivity.this,
                                                "Password updated successfully!\nPlease login again with your new password.",
                                                Toast.LENGTH_LONG).show();

                                        // Sign out and redirect to login page
                                        mAuth.signOut();
                                        finish();
                                    } else {
                                        String error = updateTask.getException() != null ?
                                                updateTask.getException().getMessage() : "Unknown error";
                                        Toast.makeText(ProfilePasswordActivity.this,
                                                "Failed to update password: " + error,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        btnUpdatePassword.setEnabled(true);
                        btnUpdatePassword.setText("Update Password");

                        String error = reAuthTask.getException() != null ?
                                reAuthTask.getException().getMessage() : "Unknown error";

                        if (error.contains("password") || error.contains("auth/wrong-password")) {
                            etCurrentPassword.setError("Current password is incorrect");
                            etCurrentPassword.requestFocus();
                        } else {
                            Toast.makeText(ProfilePasswordActivity.this,
                                    "Authentication failed: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Public static method to validate password (can be used by other activities)
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