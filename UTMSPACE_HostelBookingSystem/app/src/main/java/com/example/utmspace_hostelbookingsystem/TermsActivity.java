package com.example.utmspace_hostelbookingsystem;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class TermsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        Button btnAccept = findViewById(R.id.btnAccept);

        btnAccept.setOnClickListener(v -> {
            // Send RESULT_OK back to SignUpActivity
            setResult(RESULT_OK);
            // Close this activity
            finish();
        });
    }
}