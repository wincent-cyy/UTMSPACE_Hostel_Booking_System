package com.example.utmspace_hostelbookingsystem;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity_Debug";
    private static final String SHARED_PREFS_NAME = "BioAuthPrefs";
    private static final String KEY_BIOMETRIC_ENABLED = "FingerprintEnabled";
    private static final String KEY_SAVED_UID = "SavedUserUid";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvGoToSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPreferences;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        setupEmailAutoLowercase();

        tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            // Force a clean sign out if the user decides to type a different account manually
            if (mAuth.getCurrentUser() != null) {
                mAuth.signOut();
            }
            loginUser();
        });

        // Run checking matrix tracking variables
        checkAndTriggerBiometricAuth();
    }

    private void checkAndTriggerBiometricAuth() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String targetUid = null;

        if (currentUser != null) {
            targetUid = currentUser.getUid();
        } else {
            targetUid = sharedPreferences.getString(KEY_SAVED_UID, null);
        }

        if (targetUid != null) {
            boolean isBioEnabled = sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED + "_" + targetUid, false);
            if (isBioEnabled) {
                BiometricManager biometricManager = BiometricManager.from(this);
                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                    showBiometricPrompt(targetUid);
                }
            }
        }
    }

    private void showBiometricPrompt(String uid) {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.d(TAG, "Biometric prompt skipped or closed: " + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                if (isFinishing() || isDestroyed()) return;
                progressDialog.show();

                // If user is validated locally but active network state drops, it signs in token natively
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null && currentUser.getUid().equals(uid)) {
                    checkUserRole(currentUser.getUid());
                } else {
                    // Fallback to manual checking using verified hardware profile reference
                    checkUserRole(uid);
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginActivity.this, "Fingerprint verification failed.", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Hostel System Login")
                .setSubtitle("Scan fingerprint to securely continue to your dashboard")
                .setNegativeButtonText("Use Password Instead")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void setupEmailAutoLowercase() {
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.equals(input.toLowerCase(Locale.ROOT))) {
                    String lowercased = input.toLowerCase(Locale.ROOT);
                    etEmail.setText(lowercased);
                    etEmail.setSelection(lowercased.length());
                }
            }
        });
    }

    private void loginUser() {
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
                        if (user != null && user.isEmailVerified()) {
                            sharedPreferences.edit()
                                    .putString(KEY_SAVED_UID, user.getUid())
                                    .apply();

                            checkUserRole(user.getUid());
                        } else {
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            progressDialog.dismiss();
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        progressDialog.dismiss();
                        Log.e(TAG, "Auth failed: " + task.getException());
                        Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(String uid) {
        db.collection("Users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (timeoutRunnable != null) {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                    }

                    if (isFinishing() || isDestroyed()) return;
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String role = document.getString("role");
                            Log.d(TAG, "Role verified from Firestore: " + role);
                            if (role != null) {
                                navigateToDashboard(role);
                            } else {
                                Toast.makeText(LoginActivity.this, "Role not assigned to profile", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "User document missing for UID: " + uid);
                            Toast.makeText(LoginActivity.this, "User details not found in database.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Firestore Read Error: ", task.getException());
                        Toast.makeText(LoginActivity.this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        String structuralRole = role != null ? role.trim().toLowerCase(Locale.ROOT) : "";

        switch (structuralRole) {
            case "student":
                intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                break;
            case "staff":
                intent = new Intent(LoginActivity.this, StaffDashboardActivity.class);
                break;
            case "technician":
                intent = new Intent(LoginActivity.this, TechnicianDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                break;
            default:
                Toast.makeText(this, "Unknown role assigned: " + role, Toast.LENGTH_SHORT).show();
                return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }
}