package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private MaterialButton btnLogout;
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
        setupLogoutListener();
        setupBottomNavigation();
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Pre-select the "Profile" icon since we are on the Profile page
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void setupLogoutListener() {
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Navigate back to StudentDashboardActivity
                Intent intent = new Intent(ProfileActivity.this, StudentDashboardActivity.class);
                // Flag ensures we don't keep creating new instances of the Dashboard
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            else if (itemId == R.id.nav_profile) {
                // Already on Profile
                return true;
            }
            else if (itemId == R.id.nav_booking) {
                Toast.makeText(this, "Booking feature coming soon!", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void performLogout() {
        try {
            // Sign out from Firebase
            mAuth.signOut();

            // Clear the Activity stack and redirect to Login
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);

            // This prevents the user from clicking "Back" to see the profile after logout
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Error logging out: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}