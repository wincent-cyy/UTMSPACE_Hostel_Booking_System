package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class PaymentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnPayNow;
    private MaterialCardView cardCard, bankCard, walletCard, qrCard;

    private String selectedMethod = "";
    private MaterialCardView selectedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPayNow = findViewById(R.id.btnPayNow);

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
                Intent intent = new Intent(PaymentActivity.this, ReceiptActivity.class);
                intent.putExtra("PAYMENT_METHOD", selectedMethod);
                startActivity(intent);
            }
        });
    }

    private void selectMethod(MaterialCardView view, String method) {
        // 1. Reset previous selection
        if (selectedView != null) {
            selectedView.setStrokeWidth(0);
            selectedView.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        // 2. Set new selection
        selectedMethod = method;
        selectedView = view;

        // 3. Update UI of the selected card
        view.setStrokeColor(Color.parseColor("#6366F1"));
        view.setStrokeWidth(6); // Increased width for better visibility
        view.setCardBackgroundColor(Color.parseColor("#F1F5F9")); // Subtle highlight
    }
}