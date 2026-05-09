package com.example.utmspace_hostelbookingsystem;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton; // Import this
import androidx.appcompat.app.AppCompatActivity;

public class TermsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        // 1. Initialize the Back Arrow Button
        ImageButton btnBack = findViewById(R.id.btnBack);

        // 2. Initialize the Accept Button
        Button btnAccept = findViewById(R.id.btnAccept);

        // Function for the Back Arrow
        btnBack.setOnClickListener(v -> {
            // This simply closes the Terms page and goes back to SignUpActivity
            // The checkbox will NOT be checked because we didn't set RESULT_OK
            finish();
        });

        // Function for the Accept Button
        btnAccept.setOnClickListener(v -> {
            // Send RESULT_OK back to SignUpActivity so it can check the checkbox
            setResult(RESULT_OK);
            // Close this activity
            finish();
        });
    }
}