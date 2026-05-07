package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvStudentName;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // 1. Initialize UI Elements
        initViews();

        // 2. Setup Bottom Navigation logic
        setupBottomNavigation();

        // 3. Handle Back Press
        setupBackPress();
    }

    private void initViews() {
        tvStudentName = findViewById(R.id.tvStudentName);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Pre-select "Home" icon in the bar
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on Home
                return true;
            }
            else if (itemId == R.id.nav_history) {
                // Navigate to History/Booking page
                Intent intent = new Intent(StudentDashboardActivity.this, HistoryActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0); // Smooth transition
                return true;
            }
            else if (itemId == R.id.nav_profile) {
                // Navigate to Profile page
                Intent intent = new Intent(StudentDashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0); // Smooth transition
                return true;
            }

            return false;
        });
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Exits the app completely from the dashboard
                finishAffinity();
            }
        });
    }
}