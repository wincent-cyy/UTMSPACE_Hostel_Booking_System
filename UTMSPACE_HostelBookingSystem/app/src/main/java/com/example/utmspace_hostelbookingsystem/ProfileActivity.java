package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private MaterialButton btnLogout, btnDeleteAccount;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigation;
    private String userRole = "student"; // Default role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        fetchUserRole(); // Identify if the user is Staff or Student
        setupListeners();
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void fetchUserRole() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("Users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Assuming your Firestore field is named "role" (e.g., "staff" or "student")
                            String role = documentSnapshot.getString("role");
                            if (role != null) {
                                userRole = role.toLowerCase();
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching role", e));
        }
    }

    private void setupListeners() {
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_profile) return true;

                Intent intent = null;
                if (itemId == R.id.nav_home) {
                    // Smart Redirection based on role
                    if (userRole.equals("staff")) {
                        intent = new Intent(this, StaffDashboardActivity.class);
                    } else if (userRole.equals("technician")){
                        intent = new Intent(this, TechnicianDashboardActivity.class);
                    } else {
                        intent = new Intent(this, StudentDashboardActivity.class);
                    }
                } else if (itemId == R.id.nav_booking) {
                    intent = new Intent(this, BookingsActivity.class);
                } else if (itemId == R.id.nav_history) {
                    intent = new Intent(this, HistoryActivity.class);
                }

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }

        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure? This will delete your profile and all data permanently. You must have logged in recently to perform this action.")
                .setPositiveButton("Delete Forever", (dialog, which) -> deleteUserAccountWithData())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_delete)
                .show();
    }

    private void deleteUserAccountWithData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        db.collection("Users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    user.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ProfileActivity.this, "Account and data deleted successfully.", Toast.LENGTH_LONG).show();
                                    navigateToLogin();
                                } else {
                                    if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                        Toast.makeText(this, "Sensitive action! Please log out and login again to verify your identity.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore Delete Error: ", e);
                    Toast.makeText(this, "Failed to delete data. Please check your connection.", Toast.LENGTH_LONG).show();
                });
    }

    private void performLogout() {
        try {
            mAuth.signOut();
            Toast.makeText(this, "Logout successfully", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        } catch (Exception e) {
            Log.e(TAG, "Logout Error", e);
            Toast.makeText(this, "Error during logout: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}