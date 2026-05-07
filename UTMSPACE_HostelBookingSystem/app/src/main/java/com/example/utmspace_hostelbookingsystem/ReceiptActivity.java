package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class ReceiptActivity extends AppCompatActivity {

    private MaterialButton btnDownload, btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnDownload = findViewById(R.id.btnDownload);
        btnBackHome = findViewById(R.id.btnBackHome);
    }

    private void setupListeners() {
        // Handle Download PDF button
        btnDownload.setOnClickListener(v -> {
            Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
            // PDF generation logic would go here
        });

        // Handle Home button
        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiptActivity.this, StudentDashboardActivity.class);
            // Clear activity stack so user can't "back" into the receipt
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}