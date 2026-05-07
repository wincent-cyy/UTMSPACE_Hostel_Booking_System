package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private MaterialButton btnLogout, btnDeleteAccount;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize UI Components
        initViews();

        // 3. Set up Listeners
        setupListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Pre-select the "Profile" icon since we are on the Profile page
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void setupListeners() {
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }

        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(ProfileActivity.this, StudentDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            else if (itemId == R.id.nav_profile) {
                // Already on Profile
                return true;
            }
            else if (itemId == R.id.nav_history) {
                // Navigate to History page
                Intent intent = new Intent(ProfileActivity.this, HistoryActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }

    /**
     * Shows a confirmation dialog before deleting the account.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Deletes the current user from Firebase Authentication.
     */
    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                        } else {
                            // Often fails if the user hasn't logged in recently (Security requirement)
                            Toast.makeText(ProfileActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void performLogout() {
        try {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        } catch (Exception e) {
            Toast.makeText(this, "Error logging out: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}