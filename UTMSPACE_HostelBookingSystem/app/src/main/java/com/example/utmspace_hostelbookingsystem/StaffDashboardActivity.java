package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast; // Added for feedback
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StaffDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge removed to fix the "cut off" layout issue
        setContentView(R.layout.activity_staff_dashboard);

        // 1. Initialize the view
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 2. Setup the listener
        setupNavigation();
    }

    private void setupNavigation() {
        // Set Home as default selected
        bottomNavigationView.setSelectedItemId(R.id.nav_staff_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_staff_home) {
                // Already here, do nothing
                return true;
            }

            else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                // Brings the existing activity state forward safely instead of destroying/recreating it
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }

            else if (id == R.id.nav_staff_bookings) {
                Intent intent = new Intent(this, BookingManagementActivity.class);
                // Brings the existing activity state forward safely instead of destroying/recreating it
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }

            else if (id == R.id.nav_rooms) {
                Intent intent = new Intent(this, StaffRoomListActivity.class);
                // Brings the existing activity state forward safely instead of destroying/recreating it
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Synchronization safeguard: Re-forces the highlight ring state whenever returning back to home focal points
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_staff_home);
        }
    }
}