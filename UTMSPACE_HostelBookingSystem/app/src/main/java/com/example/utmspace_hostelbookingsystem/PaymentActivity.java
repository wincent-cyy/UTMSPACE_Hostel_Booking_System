package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class PaymentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnPayNow;
    private MaterialCardView cardCard, bankCard, walletCard, qrCard;
    private TextView tvDisplayPrice;

    private String selectedMethod = "";
    private MaterialCardView selectedView = null;

    // Variables to hold data passed from History
    private String roomName;
    private String price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment); // 1. Set layout first

        initViews();           // 2. Initialize (find the IDs)
        setupClickListeners(); // 3. Set listeners (this was crashing because initViews failed)
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPayNow = findViewById(R.id.btnPayNow);

        // CRITICAL: Double-check these IDs match your XML exactly!
        cardCard = findViewById(R.id.MethodCard);
        bankCard = findViewById(R.id.MethodBank);
        walletCard = findViewById(R.id.MethodWallet);
        qrCard = findViewById(R.id.MethodQR);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        cardCard.setOnClickListener(v -> selectMethod(cardCard, "Credit/Debit Card"));
        bankCard.setOnClickListener(v -> selectMethod(bankCard, "Online Banking"));
        walletCard.setOnClickListener(v -> selectMethod(walletCard, "E-Wallet"));
        qrCard.setOnClickListener(v -> selectMethod(qrCard, "QR Pay"));

        btnPayNow.setOnClickListener(v -> {
            if (selectedMethod.isEmpty()) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            } else {
                // Navigate to Receipt
                Intent intent = new Intent(PaymentActivity.this, ReceiptActivity.class);

                // Pass all details to the receipt
                intent.putExtra("PAYMENT_METHOD", selectedMethod);
                intent.putExtra("ROOM_NAME", roomName);
                intent.putExtra("PRICE", price);

                startActivity(intent);

                // Finish this activity so the user can't "Go Back" to pay again
                finish();
            }
        });
    }

    private void selectMethod(MaterialCardView view, String method) {
        // Reset previous selection
        if (selectedView != null) {
            selectedView.setStrokeWidth(0);
            selectedView.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        // Set new selection
        selectedMethod = method;
        selectedView = view;

        // Update UI
        view.setStrokeColor(Color.parseColor("#6366F1"));
        view.setStrokeWidth(6);
        view.setCardBackgroundColor(Color.parseColor("#F1F5F9"));
    }
}