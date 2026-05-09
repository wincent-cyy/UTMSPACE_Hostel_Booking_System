package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class PaymentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnPayNow;
    private TextView tvTotalMain;

    // References for the Cards and RadioButtons
    private MaterialCardView cardDebit, cardBank, cardWallet;
    private RadioButton rbDebit, rbBank, rbWallet;

    private String roomName;
    private String price;
    private String selectedMethod = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Retrieve data passed from History
        roomName = getIntent().getStringExtra("ROOM_NAME");
        price = getIntent().getStringExtra("PRICE");

        initViews();
        setupListeners();

        if (price != null) {
            tvTotalMain.setText(price);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPayNow = findViewById(R.id.btnPayNow);
        tvTotalMain = findViewById(R.id.tvTotalMain);

        // Find RadioButtons
        rbDebit = findViewById(R.id.rbCard);
        rbBank = findViewById(R.id.rbBank);
        rbWallet = findViewById(R.id.rbWallet);

        // Find the CardViews (Parent of the Parent of the RadioButton)
        cardDebit = (MaterialCardView) rbDebit.getParent().getParent();
        cardBank = (MaterialCardView) rbBank.getParent().getParent();
        cardWallet = (MaterialCardView) rbWallet.getParent().getParent();
    }

    private void setupListeners() {
        // 1. Back Arrow navigation
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, HistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 2. Custom Radio Logic: Handle Card Clicks
        cardDebit.setOnClickListener(v -> updateRadioSelection(rbDebit, "Credit / Debit Card"));
        cardBank.setOnClickListener(v -> updateRadioSelection(rbBank, "Online Banking (FPX)"));
        cardWallet.setOnClickListener(v -> updateRadioSelection(rbWallet, "E-Wallet"));

        // 3. Custom Radio Logic: Handle RadioButton Direct Clicks
        rbDebit.setOnClickListener(v -> updateRadioSelection(rbDebit, "Credit / Debit Card"));
        rbBank.setOnClickListener(v -> updateRadioSelection(rbBank, "Online Banking (FPX)"));
        rbWallet.setOnClickListener(v -> updateRadioSelection(rbWallet, "E-Wallet"));

        // 4. Pay Now Button
        btnPayNow.setOnClickListener(v -> {
            if (selectedMethod.isEmpty()) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(PaymentActivity.this, ReceiptActivity.class);
                intent.putExtra("PAYMENT_METHOD", selectedMethod);
                intent.putExtra("ROOM_NAME", roomName != null ? roomName : "N/A");
                intent.putExtra("PRICE", price != null ? price : "0.00");

                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * This method ensures only ONE radio button is checked at a time
     * and handles the visual selection.
     */
    private void updateRadioSelection(RadioButton targetRadio, String method) {
        // Uncheck all first
        rbDebit.setChecked(false);
        rbBank.setChecked(false);
        rbWallet.setChecked(false);

        // Check the target
        targetRadio.setChecked(true);
        selectedMethod = method;
    }
}