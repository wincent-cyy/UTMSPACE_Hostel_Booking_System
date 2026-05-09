package com.example.utmspace_hostelbookingsystem;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // REMOVED: EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        // REMOVED: The WindowInsetsListener is no longer needed
        // if you aren't using EdgeToEdge.
    }
}