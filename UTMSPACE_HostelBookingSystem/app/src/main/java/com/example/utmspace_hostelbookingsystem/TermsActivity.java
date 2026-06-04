package com.example.utmspace_hostelbookingsystem;

import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton; // Import this
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class TermsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        // 2. Initialize the Accept Button
        Button btnAccept = findViewById(R.id.backToLoginFromTermsBtn);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        // Function for the Accept Button
        btnAccept.setOnClickListener(v -> {
            // Send RESULT_OK back to SignUpActivity so it can check the checkbox
            setResult(RESULT_OK);
            // Close this activity
            finish();
        });
    }
}