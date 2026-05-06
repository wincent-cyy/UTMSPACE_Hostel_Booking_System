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

        // 3. Handle Back Press (Exits app from Dashboard instead of going back to Login)
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
            else if (itemId == R.id.nav_profile) {
                // Navigate to Profile page when the bar icon is clicked
                Intent intent = new Intent(StudentDashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            else if (itemId == R.id.nav_booking) {
                Toast.makeText(this, "Booking feature coming soon!", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void setupBackPress() {
        // This ensures that if the user presses 'Back' on the Dashboard,
        // the app closes entirely rather than returning to the Login screen.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }
}