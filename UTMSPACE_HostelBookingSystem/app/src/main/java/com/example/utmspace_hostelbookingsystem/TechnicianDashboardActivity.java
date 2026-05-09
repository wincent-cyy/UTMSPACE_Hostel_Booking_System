package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast; // Added for feedback
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TechnicianDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge removed to fix the "cut off" layout issue
        setContentView(R.layout.activity_technician_dashboard);

        // 1. Initialize the view
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 2. Setup the listener
        setupNavigation();
    }

    private void setupNavigation() {
        // Set Home as default selected
        bottomNavigationView.setSelectedItemId(R.id.nav_tech_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_tech_home) {
                // Already here, do nothing
                return true;
            }

            else if (id == R.id.nav_profile) {
                // Profile is ready, go there
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }

            else if (id == R.id.nav_request || id == R.id.nav_tech_history) {
                // These are not ready yet
                Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
                return false; // Return false so the icon doesn't look "selected"
            }

            return false;
        });
    }
}